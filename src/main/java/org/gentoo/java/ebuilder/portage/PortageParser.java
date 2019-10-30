package org.gentoo.java.ebuilder.portage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gentoo.java.ebuilder.Config;

/**
 * Parses portage tree and collects information needed for generating ebuilds.
 *
 * @author fordfrog
 */
public class PortageParser {

    /**
     * Cache version.
     */
    public static final String CACHE_VERSION = "1.0";
    /**
     * Current ant utilities eclass name.
     */
    private static final String ECLASS_ANT_TASKS = "ant-tasks";
    /**
     * Current java package eclass name.
     */
    private static final String ECLASS_JAVA_PKG = "java-pkg-2";
    /**
     * Current java optional package eclass name.
     */
    private static final String ECLASS_JAVA_PKG_OPT = "java-pkg-opt-2";
    /**
     * Current java simple package eclass name.
     */
    private static final String ECLASS_JAVA_PKG_SIMPLE = "java-pkg-simple";
    /**
     * Current java utilities eclass name.
     */
    private static final String ECLASS_JAVA_UTILS = "java-utils-2";
    /**
     * Pattern for parsing SLOT with bash substring.
     */
    private static final Pattern PATTERN_SLOT_SUBSTRING = Pattern.compile(
            "^\\$\\{PV:(\\d+):(\\d+)\\}$");
    /**
     * Pattern for parsing version component range in SLOT.
     */
    private static final Pattern PATTERN_SLOT_VERSION_COMPOPONENT_RANGE
            = Pattern.compile(
                    "^\\$\\(get_version_component_range (\\d+)-(\\d+)\\)$");
    /**
     * Pattern for checking whether the line contains variable declaration. It
     * does not handle correctly variables spread across several lines but we
     * most probably do not care about these.
     */
    private static final Pattern PATTERN_VARIABLE = Pattern.compile(
            "^(\\S+?)=(.*)$");
    /**
     * List of cache items. This list is populated during parsing the tree.
     */
    private final List<CacheItem> cacheItems = new ArrayList<>(40_000);
    /**
     * Number of processed categories. Updated during parsing the tree.
     */
    private int processedCategories;
    /**
     * Number of processed ebuilds. Updated during parsing the tree.
     */
    private int processedEbuilds;
    /**
     * Number of processed packages. Updated during parsing the tree.
     */
    private int processedPackages;

    /**
     * Parses portage tree at specified path and create ebuild cache at
     * ~/.java-ebuilder/cache.
     *
     * @param config application configuration
     */
    public void parseTree(final Config config) {
        final long startTimestamp = System.currentTimeMillis();
        cacheItems.clear();
        processedCategories = 0;
        processedPackages = 0;
        processedEbuilds = 0;

        config.getStdoutWriter().println("Parsing portage tree @ "
                + config.getPortageTree() + " ...");
        parseCategories(config.getPortageTree());

        final long endTimestamp = System.currentTimeMillis();

        config.getStdoutWriter().println(MessageFormat.format(
                "Parsed {0} categories {1} packages {2} ebuilds in {3}ms and "
                + "found {4} java ebuilds",
                processedCategories, processedPackages, processedEbuilds,
                endTimestamp - startTimestamp, cacheItems.size()));

        config.getStdoutWriter().print("Writing cache file...");
        writeCacheFile(config);
        config.getStdoutWriter().println("done");
    }

    /**
     * Extracts the most important java eclass from ebuild inherit line.
     *
     * @param inheritLine ebuild inherit line
     *
     * @return java eclass or null
     */
    private String getJavaInheritEclass(final String inheritLine) {
        if (inheritLine.contains(ECLASS_JAVA_PKG)) {
            return ECLASS_JAVA_PKG;
        } else if (inheritLine.contains(ECLASS_JAVA_PKG_OPT)) {
            return ECLASS_JAVA_PKG_OPT;
        } else if (inheritLine.contains(ECLASS_JAVA_PKG_SIMPLE)) {
            return ECLASS_JAVA_PKG_SIMPLE;
        } else if (inheritLine.contains(ECLASS_JAVA_UTILS)) {
            return ECLASS_JAVA_UTILS;
        } else if (inheritLine.contains(ECLASS_ANT_TASKS)) {
            return ECLASS_ANT_TASKS;
        } else {
            return null;
        }
    }

    /**
     * Parses categories in the portage tree root.
     *
     * @param treePath portage tree path
     */
    private void parseCategories(final Path treePath) {
        final File[] categories = treePath.toFile().listFiles(
                (final File pathname) -> pathname.isDirectory());

        for (final File category : categories) {
            parseCategory(category);
            processedCategories++;
        }
    }

    /**
     * Parses category and its packages.
     *
     * @param category category path
     */
    private void parseCategory(final File category) {
        final File[] packages = category.listFiles(
                (final File pathname) -> pathname.isDirectory());

        for (final File pkg : packages) {
            parsePackage(pkg);
            processedPackages++;
        }
    }

    /**
     * Parses single ebuild.
     *
     * @param ebuild ebuild path
     */
    private void parseEbuild(final File ebuild) {
        final String filename = ebuild.getName().replaceAll("\\.ebuild$", "");
        final String category = ebuild.getParentFile().getParentFile().getName();
        final String pkg = ebuild.getParentFile().getName();
        final String version = filename.substring(pkg.length() + 1);
        final Map<String, String> variables = new HashMap<>(20);
        String eclass = null;
        String slot = "0";
        String useFlag = null;
        String mavenId = null;
        String groupId = null;
        String artifactId = null;
        String mavenVersion = null;

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(ebuild.toPath(),
                        StandardOpenOption.READ)))) {
            String line = reader.readLine();

            while (line != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    final int pos = line.indexOf('#');

                    if (pos != -1) {
                        line = line.substring(0, pos).trim();
                    }
                }

                if (!line.isEmpty()) {
                    final Matcher matcher = PATTERN_VARIABLE.matcher(line);

                    if (matcher.matches()) {
                        variables.put(matcher.group(1), matcher.group(2).
                                replaceAll("(^\"|\"$)", ""));
                    }

                    if (line.startsWith("inherit ")) {
                        eclass = getJavaInheritEclass(line);

                        if (eclass == null) {
                            return;
                        }
                    } else if (line.startsWith("SLOT=")) {
                        slot = line.substring("SLOT=".length()).replace(
                                "\"", "").replaceAll("/.*", "");
                    } else if (line.startsWith("JAVA_PKG_OPT_USE=")) {
                        useFlag = line.substring("JAVA_PKG_OPT_USE=".length()).
                                replace("\"", "");
                    } else if (line.startsWith("MAVEN_ID=")) {
                        mavenId = line.substring("MAVEN_ID=".length()).
                                replace("\"", "");
                    }
                }

                line = reader.readLine();
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to read ebuild", ex);
        }

        if (eclass == null) {
            return;
        }

        if (ECLASS_JAVA_PKG_OPT.equals(eclass) && useFlag == null) {
            useFlag = "java";
        }

        final String pv;
        final int pos = version.indexOf('-');

        if (pos == -1) {
            pv = version;
        } else {
            pv = version.substring(0, pos);
        }

        slot = processSlot(slot, pv, variables);

        if (mavenId != null) {
            mavenId = mavenId.replaceAll("\\$(\\{PN\\}|PN)", pkg).
                    replaceAll("\\$(\\{PV\\}|PV)", pv);

            final String[] parts = mavenId.split(":");

            if (parts[0].isEmpty()) {
                groupId = pkg;
            } else {
                groupId = parts[0];
            }

            if (parts.length > 1) {
                artifactId = parts[1];
            } else {
                artifactId = pkg;
            }

            if (parts.length > 2) {
                mavenVersion = parts[2];
            } else {
                mavenVersion = version;
            }
        }

        cacheItems.add(new CacheItem(category, pkg, version, slot, useFlag,
                groupId, artifactId, mavenVersion));
    }

    /**
     * Parses package and its ebuilds.
     *
     * @param pkg package path
     */
    private void parsePackage(final File pkg) {
        final File[] ebuilds = pkg.listFiles(
                (final File pathname) -> pathname.isFile()
                && pathname.getName().endsWith(".ebuild"));

        for (final File ebuild : ebuilds) {
            parseEbuild(ebuild);
            processedEbuilds++;
        }
    }

    /**
     * Processes various instructions in SLOT string.
     *
     * @param slot      SLOT string
     * @param pv        PV variable
     * @param variables map of collected variables and their values
     *
     * @return processed SLOT string
     */
    private String processSlot(final String slot, final String pv,
            final Map<String, String> variables) {
        String result = slot.replaceAll("\\$(\\{PV\\}|PV)", pv);

        if (result.indexOf('$') != -1) {
            for (final Map.Entry<String, String> variable
                    : variables.entrySet()) {
                result = result.
                        replace("$" + variable.getKey(), variable.getValue()).
                        replace("${" + variable.getKey() + '}',
                                variable.getValue());
            }
        }

        if (result.indexOf('$') != -1) {
            final Matcher matcher = PATTERN_SLOT_SUBSTRING.matcher(result);

            if (matcher.matches()) {
                final int start = Integer.parseInt(matcher.group(1), 10);
                final int length = Integer.parseInt(matcher.group(2), 10);
                result = pv.substring(start, start + length);
            }
        }

        if (result.indexOf('$') != -1) {
            final Matcher matcher = PATTERN_SLOT_VERSION_COMPOPONENT_RANGE.
                    matcher(result);

            if (matcher.matches()) {
                final int start = Integer.parseInt(matcher.group(1), 10);
                final int end = Integer.parseInt(matcher.group(2), 10);
                final String[] parts = pv.split("\\.");
                final StringBuilder sbResult = new StringBuilder(10);

                for (int i = start; i <= end; i++) {
                    if (sbResult.length() > 0) {
                        sbResult.append('.');
                    }

                    sbResult.append(i <= parts.length ? parts[i - 1] : '0');
                }

                result = sbResult.toString();
            }
        }

        return result;
    }

    /**
     * Writes cache items to the cache file.
     *
     * @param config application configuration
     */
    private void writeCacheFile(final Config config) {
        final File cacheDir = config.getCacheFile().getParent().toFile();

        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        cacheItems.sort((
                final CacheItem o1,
                final CacheItem o2) -> {
            if (o1.getCategory().compareTo(o2.getCategory()) != 0) {
                return o1.getCategory().compareTo(o2.getCategory());
            } else if (o1.getPkg().compareTo(o2.getPkg()) != 0) {
                return o1.getPkg().compareTo(o2.getPkg());
            } else {
                return o1.getVersion().compareTo(o2.getVersion());
            }
        });

        try (final OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(config.getCacheFile(),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING),
                Charset.forName("UTF-8"))) {
            writer.write(CACHE_VERSION);
            writer.write("\n#category:pkg:version:slot:useFlag:groupId:"
                    + "artifactId:mavenVersion\n");

            for (final CacheItem cacheItem : cacheItems) {
                writer.write(cacheItem.getCategory());
                writer.write(':');
                writer.write(cacheItem.getPkg());
                writer.write(':');
                writer.write(cacheItem.getVersion());
                writer.write(':');
                writer.write(cacheItem.getSlot());
                writer.write(':');
                writer.write(cacheItem.getUseFlag() == null
                        ? "" : cacheItem.getUseFlag());

                if (cacheItem.getGroupId() != null) {
                    writer.write(':');
                    writer.write(cacheItem.getGroupId());
                    writer.write(':');
                    writer.write(cacheItem.getArtifactId());

                    if (cacheItem.getMavenVersion() != null) {
                        writer.write(':');
                        writer.write(cacheItem.getMavenVersion());
                    }
                }

                writer.write('\n');
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to write cache file @ "
                    + config.getCacheFile(), ex);
        }
    }
}
