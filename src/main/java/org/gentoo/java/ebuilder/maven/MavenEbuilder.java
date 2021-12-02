package org.gentoo.java.ebuilder.maven;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import org.gentoo.java.ebuilder.Config;
import org.gentoo.java.ebuilder.portage.MavenVersion;

/**
 * Generates ebuild from maven project.
 *
 * @author fordfrog
 */
public class MavenEbuilder {

    /**
     * The fallback description if no description is found in pom.xml.
     */
    private static final String defaultDescription = "${MAVEN_ID}";

    /**
     * The fallback homepage if no homepage is found in pom.xml.
     */
    private static final String defaultHomepage
            = "https://wiki.gentoo.org/wiki/No_homepage";

    /**
     * EAPI version.
     */
    private static final String EAPI = "8";
    /**
     * Pattern for retrieval of tarball extension.
     */
    private static final Pattern PATTERN_TARBALL_EXTENSION = Pattern.compile(
            "^.*((?:\\.tar)\\.\\S+|(?:\\.jar))$");
    /**
     * Pattern for checking whether download tarball name matches expected name.
     */
    private static final Pattern PATTERN_TARBALL_NAME
            = Pattern.compile("^.*/\\$\\{P\\}-sources((?:\\.tar)\\.\\S+|(?:\\.jar))$");

    /**
     * Pattern for checking whether download tarball for testing name matches expected name.
     */
    private static final Pattern PATTERN_TEST_TARBALL_NAME
            = Pattern.compile("^.*/\\$\\{P\\}-test-sources\\.jar$");

    /**
     * Pattern for checking whether the dependency is specifying versions.
     */
    private static final Pattern PATTERN_EBUILD_VERSIONING
            = Pattern.compile("^[~=<>].*$");

    /**
     * Generates ebuild from the collected information at the specified path.
     *
     * @param config        application configuration
     * @param mavenProjects list of maven project information
     * @param mavenCache    populated maven cache
     */
    public void generateEbuild(final Config config,
            final List<MavenProject> mavenProjects,
            final MavenCache mavenCache) {
        config.getStdoutWriter().print("Writing ebuild...");

        try (final PrintWriter writer = new PrintWriter(
                new FileWriter(config.getEbuild().toFile()))) {
            writeHeader(writer);
            writeCommand(config, writer);
            writeEAPI(writer);

            /**
             * Write the info from the last project as it is probably the one
             * that depends on the rest.
             */
            final MavenProject mavenProject
                    = mavenProjects.get(mavenProjects.size() - 1);
            writeInherit(config, mavenProject, writer);
            writePackageInfo(config, mavenProject, writer);

            writeDependencies(config, mavenProjects, writer);
            writeSourceDir(writer);
            writeScript(config, mavenProjects, writer);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to write ebuild", ex);
        }

        config.getStdoutWriter().println("done");
    }

    /**
     * Creates classpath string from provided dependencies.
     *
     * @param dependencies list of dependencies
     *
     * @return classpath
     */
    private String createClassPath(
            final List<MavenDependency> dependencies) {
        final StringBuilder sbCP = new StringBuilder(dependencies.size() * 15);

        dependencies.stream().filter((dependency)
                -> dependency.getSystemDependency() != null).
                forEach((dependency) -> {
                    if (sbCP.length() > 0) {
                        sbCP.append(',');
                    }

                    final String ebuildDependency =
                            dependency.getSystemDependency();

                    final String[] parts = ebuildDependency.
                            replaceAll(".*/", "").
                            replaceAll("\\[.*\\]", "").
                            split(":");
                    String pn = parts[0].replaceAll("-r\\d+$", "");

                    if (parts.length == 2) {
                        if (PATTERN_EBUILD_VERSIONING.
                                matcher(ebuildDependency).matches()) {
                            pn = pn.substring(0, pn.lastIndexOf('-'));
                        }

                        if (!parts[1].equals("0")) {
                            pn += "-" + parts[1];
                        }
                    }

                    sbCP.append(pn);
                });

        return sbCP.toString();
    }

    /**
     * Determines the testing framework based on project dependencies.
     *
     * @param mavenProjects list of maven projects
     *
     * @return testing framework name or null
     */
    private String determineTestingFramework(
            final List<MavenProject> mavenProjects, final Config config) {
        for (final MavenProject mavenProject : mavenProjects) {
            final String result
                    = determineTestingFramework(mavenProject, config);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Determines the testing framework based on project dependencies.
     *
     * @param mavenProject maven project
     *
     * @return testing framework name or null
     */
    private String determineTestingFramework(
            final MavenProject mavenProject, final Config config) {
        Set<String> frameworks = new HashSet<>(10);

        if (mavenProject.hasTests()) {
            for (final MavenDependency dependency : mavenProject.
                    getTestDependencies()) {
                frameworks.add(determineTestingFrameworkByDependency(dependency));
            }

            for (final MavenDependency dependency : mavenProject.
                    getCommonDependencies()) {
                frameworks.add(determineTestingFrameworkByDependency(dependency));
            }
        }

        if (config.hasBinjarUri()) {
            frameworks.add("pkgdiff");
        }

        frameworks.remove(null);

        if (frameworks.size() == 0) {
            return null;
        } else {
            return String.join(" ", frameworks);
        }
    }

    /**
     * Determines the testing framework based on project dependencies.
     *
     * @param mavenProject maven project
     *
     * @return testing framework name or null
     */
    private String determineTestingFrameworkByDependency(
            final MavenDependency dependency) {
        /** TODO: missing determination for
         *    "POJO" tests
         *    "spock" tests
         */
        if ("junit".equals(dependency.getGroupId())
                && "junit".equals(dependency.getArtifactId())) {
            if (dependency.getMavenVersion().
                    compareTo(new MavenVersion("3.9.9")) < 1) {
                return "junit";
            } else {
                return "junit-4";
            }
        } else if ("org.testng".equals(dependency.getGroupId())
                && "testng".equals(dependency.getArtifactId())) {
            return "testng";
        } else if ("org.junit.jupiter".equals(dependency.getGroupId())
                && "junit-jupiter-engine".equals(dependency.getArtifactId())) {
            // java-pkg-simple does not support this framework
            return "junit-jupiter";
        } else if ("org.junit.vintage".equals(dependency.getGroupId())
                && "junit-vintage-engine".equals(dependency.getArtifactId())) {
            // java-pkg-simple does not support this framework
            return "junit-vintage";
        } else if ("io.cucumber".equals(dependency.getGroupId())
                && "cucumber-junit".equals(dependency.getArtifactId())) {
            // java-pkg-simple does not support this framework
            return "cucumber";
        } else {
            return null;
        }
    }

    /**
     * Retrieves minimum source version from the maven projects.
     *
     * @param mavenProjects   list of maven projects
     * @param forceMinVersion optional minimum version to force
     *
     * @return minimum source version
     */
    private JavaVersion getMinSourceVersion(
            final List<MavenProject> mavenProjects,
            final JavaVersion forceMinVersion) {
        JavaVersion result = null;

        for (final MavenProject mavenProject : mavenProjects) {
            if (result == null || mavenProject.getSourceVersion().compareTo(
                    result) < 0) {
                result = mavenProject.getSourceVersion();
            }
        }

        if (forceMinVersion != null && forceMinVersion.compareTo(result) > 0) {
            return forceMinVersion;
        }

        return result;
    }

    /**
     * Retrieves minimum target version from the maven projects.
     *
     * @param mavenProjects   list of maven projects
     * @param forceMinVersion optional minimum version to force
     *
     * @return minimum target version
     */
    private JavaVersion getMinTargetVersion(
            final List<MavenProject> mavenProjects,
            final JavaVersion forceMinVersion) {
        JavaVersion result = null;

        for (final MavenProject mavenProject : mavenProjects) {
            if (result == null || mavenProject.getTargetVersion().compareTo(
                    result) < 0) {
                result = mavenProject.getTargetVersion();
            }
        }

        if (forceMinVersion != null && forceMinVersion.compareTo(result) > 0) {
            return forceMinVersion;
        }

        return result;
    }

    /**
     * If the tarball name does not match pattern ${P}.ext then we will update
     * it to store the tarball as ${P}.ext.
     *
     * @param srcUri source URI
     *
     * @return either original source URI or updated source URI
     */
    private String improveSrcUri(final String srcUri) {
        if (PATTERN_TARBALL_NAME.matcher(srcUri).matches()) {
            return srcUri;
        }

        final Matcher matcher = PATTERN_TARBALL_EXTENSION.matcher(srcUri);

        /**
         * We do not know how to get the extension so we will leave the tarball
         * name as it is.
         */
        if (!matcher.matches()) {
            return srcUri;
        }

        return srcUri + " -> " + "${P}-sources" + matcher.group(1);
    }

    /**
     * Rename binjar file to ${P}-bin.ext
     *
     * @param binjarUri binjar URI
     *
     * @return updated binjar URI
     */
    private String improveBinjarUri(final String binjarUri) {

        /**
         * Binary file should be jars
         */
        return binjarUri + " -> " + "${P}-bin.jar";

    }

    /**
     * If the tarball name does not match pattern ${P}-test-sources.jar
     * we will update it to store the tarball as ${P}-test-sources.jar.
     * Note that we only need it for Maven central artifacts, so it is
     * safe to assume that the extension should be ".jar".
     *
     * @param TestSrcUri source test URI
     *
     * @return either original source test URI or updated source test URI
     */
    private String improveTestSrcUri(final String TestSrcUri) {
        if (PATTERN_TEST_TARBALL_NAME.matcher(TestSrcUri).matches()) {
            return TestSrcUri;
        }

        return TestSrcUri + " -> " + "${P}-test-sources.jar";
    }

    /**
     * Merges maven project system dependencies of specified type and removed
     * duplicates.
     *
     * @param mavenProjects list of maven projects
     * @param type          type of dependencies ("common", "compile", "runtime"
     *                      and "test")
     *
     * @return list of merged dependencies
     */
    private List<String> mergeSystemDependencies(
            final List<MavenProject> mavenProjects, final String type) {
        final List<String> result = new ArrayList<>(30);

        mavenProjects.stream().forEach((mavenProject) -> {
            final List<MavenDependency> dependencies;

            switch (type) {
                case "common":
                    dependencies = mavenProject.getCommonDependencies();
                    break;
                case "compile":
                    dependencies = mavenProject.getCompileDependencies();
                    break;
                case "runtime":
                    dependencies = mavenProject.getRuntimeDependencies();
                    break;
                case "test":
                    dependencies = mavenProject.getTestDependencies();
                    break;
                default:
                    throw new RuntimeException(
                            "Dependencies type not supported: " + type);
            }

            dependencies.stream().filter((dependency)
                    -> (dependency.getSystemDependency() != null
                    && !result.contains(dependency.getSystemDependency())
                    && (determineTestingFrameworkByDependency(dependency) == null
                        || type != "test"))).
                    forEach((dependency) -> {
                        result.add(dependency.getSystemDependency());
                    });
        });

        result.sort((final String o1, final String o2) -> {
            return o1.compareTo(o2);
        });

        return result;
    }

    private String replaceWithVars(final String string, final Config config) {
        final String pString
                = config.getEbuildName() + '-' + config.getEbuildVersion();

        return string.
                replace(pString, "${P}").
                replace(config.getEbuildName(), "${PN}").
                replace(config.getEbuildVersion(), "${PV}");
    }

    /**
     * Writes command that was used to create skeleton of the ebuild.
     *
     * @param config application configuration
     * @param writer ebuild writer
     */
    private void writeCommand(final Config config, final PrintWriter writer) {
        writer.println();
        writer.println("# Skeleton command:");
        writer.print("# java-ebuilder --generate-ebuild --workdir .");

        if (!config.getPomFiles().isEmpty()) {
            config.getPomFiles().stream().forEach((pomFile) -> {
                writer.print(" --pom ");
                writer.print(pomFile);
            });
        }

        if (config.isFromMavenCentral()) {
            writer.print(" --from-maven-central");
        }

        if (config.getDownloadUri() != null) {
            writer.print(" --download-uri ");
            writer.print(config.getDownloadUri());
        }

        if (config.hasBinjarUri()) {
            writer.print(" --binjar-uri ");
            writer.print(config.getBinjarUri());
        }

        if (config.hasTestSrcUri()) {
            writer.print(" --test-src-uri ");
            writer.print(config.getTestSrcUri());
        }

        if (config.getLicense() != null) {
            writer.print(" --license ");
            writer.print(config.getLicense());
        }

        if (config.getSlot() != null) {
            writer.print(" --slot ");
            writer.print(config.getSlot());
        }

        if (config.getKeywords() != null) {
            writer.print(" --keywords \"");
            writer.print(config.getKeywords());
            writer.print("\"");
        }

        if (config.getEbuild() != null) {
            writer.print(" --ebuild ");
            writer.print(config.getEbuild().getFileName());
        }

        if (config.getForceMinJavaVersion() != null) {
            writer.print(" --force-min-java-version ");
            writer.print(config.getForceMinJavaVersion());
        }

        writer.println();
    }

    /**
     * Writes dependencies to the ebuild.
     *
     * @param config        application configuration
     * @param mavenProjects list of maven projects
     * @param writer        ebuild writer
     */
    private void writeDependencies(final Config config,
            final List<MavenProject> mavenProjects,
            final PrintWriter writer) {
        final List<String> commonDependencies = mergeSystemDependencies(
                mavenProjects, "common");
        final List<String> testDependencies = mergeSystemDependencies(
                mavenProjects, "test");
        final List<String> compileDependencies = mergeSystemDependencies(
                mavenProjects, "compile");
        final List<String> runtimeDependencies = mergeSystemDependencies(
                mavenProjects, "runtime");
        final boolean hasCDepend = !commonDependencies.isEmpty();

        if (hasCDepend) {
            writer.println("\n# Common dependencies");

            mavenProjects.stream().
                    filter((mavenProject)
                            -> !mavenProject.getCommonDependencies().isEmpty()).
                    forEach((mavenProject) -> {
                        writeDependenciesInfo(config, writer,
                                mavenProject.getPomFile(),
                                mavenProject.getCommonDependencies(), null);
                    });

            writer.println("\nCDEPEND=\"");

            commonDependencies.stream().forEach((dependency) -> {
                writer.print('\t');
                writer.println(dependency);
            });

            writer.println('"');
        }

        if (!compileDependencies.isEmpty()
                || !testDependencies.isEmpty()) {
            writer.println("\n# Compile dependencies");

            mavenProjects.stream().
                    filter((mavenProject)
                            -> !mavenProject.getCompileDependencies().isEmpty()
                    || !mavenProject.getTestDependencies().isEmpty())
                    .forEach((mavenProject) -> {
                        if (!mavenProject.getCompileDependencies().isEmpty()) {
                            writeDependenciesInfo(config, writer,
                                    mavenProject.getPomFile(),
                                    mavenProject.getCompileDependencies(),
                                    null);
                        }

                        if (!mavenProject.getTestDependencies().isEmpty()) {
                            writeDependenciesInfo(config, writer,
                                    mavenProject.getPomFile(),
                                    mavenProject.getTestDependencies(),
                                    "test?");
                        }
                    });
        }

        writer.print("\nDEPEND=\"\n\t>=virtual/jdk-");
        writer.print(getMinSourceVersion(
                mavenProjects, config.getForceMinJavaVersion()));
        writer.println(":*");

        if (config.hasBinjarUri()) {
            if (hasCDepend && compileDependencies.isEmpty()) {
                writer.println("\t!binary? ( ${CDEPEND} )");
            } else if (!compileDependencies.isEmpty()) {
                writer.println("\t!binary? (");

                if (hasCDepend) {
                    writer.println("\t\t${CDEPEND}");
                }

                if (!compileDependencies.isEmpty()) {
                    compileDependencies.stream().forEach((dependency) -> {
                        writer.print("\t\t");
                        writer.println(dependency);
                    });
                }

                writer.println("\t)");
            }
        } else {
            if (hasCDepend) {
                writer.println("\t${CDEPEND}");
            }

            if (!compileDependencies.isEmpty()) {
                compileDependencies.stream().forEach((dependency) -> {
                    writer.print('\t');
                    writer.println(dependency);
                });
            }
        }

        if (!testDependencies.isEmpty()) {
            writer.println("\ttest? (");

            testDependencies.stream().forEach((dependency) -> {
                writer.print("\t\t");
                writer.println(dependency);
            });

            writer.println("\t)");
        }

        writer.println('"');

        if (!runtimeDependencies.isEmpty()) {
            writer.println("\n# Runtime dependencies");

            mavenProjects.stream().
                    filter((mavenProject)
                            -> !mavenProject.getRuntimeDependencies().isEmpty())
                    .forEach((mavenProject) -> {
                        writeDependenciesInfo(config, writer,
                                mavenProject.getPomFile(),
                                mavenProject.getRuntimeDependencies(), null);
                    });
        }

        writer.print("\nRDEPEND=\"\n\t>=virtual/jre-");
        writer.print(getMinTargetVersion(
                mavenProjects, config.getForceMinJavaVersion()));
        writer.println(":*");

        if (hasCDepend) {
            writer.print("\t${CDEPEND}");
        }

        if (!runtimeDependencies.isEmpty()) {
            runtimeDependencies.stream().forEach((dependency) -> {
                writer.println();
                writer.print('\t');
                writer.println(dependency);
            });
        }

        writer.println('"');

        if (config.getDownloadUri() != null && config.getDownloadUri().
                toString().matches("^.*?\\.(jar|zip)$")) {
            writer.println("\nBDEPEND=\"app-arch/unzip\"");
        }
    }

    /**
     * Writes dependencies information to the ebuild.
     *
     * @param config       application configuration
     * @param writer       ebuild writer
     * @param pomFile      path to pom file
     * @param dependencies list of dependencies
     * @param useFlag      optional USE flag including question mark
     */
    private void writeDependenciesInfo(final Config config,
            final PrintWriter writer, final Path pomFile,
            final List<MavenDependency> dependencies, final String useFlag) {
        writer.print("# POM: ");
        writer.println(replaceWithVars(pomFile.toString(), config));

        dependencies.stream().forEach((dependency) -> {
            writer.print("# ");

            if (useFlag != null) {
                writer.print(useFlag);
                writer.print(' ');
            }

            writer.print(dependency.getGroupId());
            writer.print(':');
            writer.print(dependency.getArtifactId());
            writer.print(':');
            writer.print(dependency.getVersion());
            writer.print(" -> ");
            writer.println(dependency.getSystemDependency());
        });
    }

    /**
     * Writes EAPI version.
     *
     * @param writer ebuild writer
     */
    private void writeEAPI(final PrintWriter writer) {
        writer.println();
        writer.print("EAPI=");
        writer.println(EAPI);
    }

    /**
     * Writes Gentoo header.
     *
     * @param writer ebuild writer
     */
    private void writeHeader(final PrintWriter writer) {
        writer.printf("# Copyright 1999-%d Gentoo Authors\n",
                LocalDate.now().getYear());
        writer.println("# Distributed under the terms of the GNU General "
                + "Public License v2");
    }

    /**
     * Writes inherit line.
     *
     * @param writer ebuild writer
     */
    private void writeInherit(final Config config,
            final MavenProject mavenProject, final PrintWriter writer) {
        writer.println();
        writer.print("JAVA_PKG_IUSE=\"doc source");

        if (mavenProject.hasTests() || config.hasBinjarUri()) {
            writer.print(" test");
        }

        if (config.hasBinjarUri()) {
            writer.print(" binary");
        }

        writer.println('"');

        // write MAVEN_ID ahead of DESCRIPTION
        writer.print("MAVEN_ID=\"");
        writer.print(mavenProject.getGroupId());
        writer.print(':');
        writer.print(mavenProject.getArtifactId());
        writer.print(':');
        writer.print(mavenProject.getVersion());
        writer.println('"');

        // write testing framworks, so java-pkg-simple.eclass can deal with it
        final String testingFramework
                = determineTestingFramework(mavenProject, config);

        if (testingFramework != null) {
            writer.print("JAVA_TESTING_FRAMEWORKS=\"");
            writer.print(testingFramework);
            writer.println('"');
        }

        writer.println();
        writer.print("inherit java-pkg-2 java-pkg-simple");

        if (config.isFromMavenCentral()) {
            writer.print(" java-pkg-maven");
        }

        writer.println("");
    }

    /**
     * Writes ebuild script for multiple projects.
     *
     * @param config        application configuration
     * @param mavenProjects list of maven projects
     * @param writer        ebuild writer
     */
    private void writeMultipleProjectsScript(final Config config,
            final List<MavenProject> mavenProjects, final PrintWriter writer) {
        // TODO: implement multiple-project script
        throw new UnsupportedOperationException("Not implemented yet.");

        // Global:
        // JAVA_GENTOO_CLASSPATH
        // JAVA_CLASSPATH_EXTRA
        // JAVA_TEST_GENTOO_CLASSPATH
        // JAVA_ENCODING (unless it differs in projects)
        // JAVA_NEED_TOOLS
        // Compile (jars, doc):
        // JAVA_SRC_DIR
        // JAVA_RESOURCE_DIRS
        // JAVA_ENCODING (in case project encodings are different)
        // Test:
        // JAVA_TESTING_FRAMEWORKS
        // JAVA_TEST_SRC_DIR
        // JAVA_TEST_RESOURCE_DIRS
        // Install (jars, doc, sources):
        // JAVA_MAIN_CLASS
    }

    /**
     * Writes package information.
     *
     * @param config       application configuration
     * @param mavenProject maven project instance
     * @param writer       ebuild writer
     */
    private void writePackageInfo(final Config config,
            final MavenProject mavenProject, final PrintWriter writer) {
        writer.println();

        writer.print("DESCRIPTION=\"");

        if (mavenProject.getDescription() != null) {
            writer.print(mavenProject.getDescription().replace("\"", "\\\""));
        } else {
            writer.print(defaultDescription);
        }

        writer.println('"');

        writer.print("HOMEPAGE=\"");

        if (mavenProject.getHomepage() != null) {
            writer.print(mavenProject.getHomepage());
        } else {
            writer.print(defaultHomepage);
        }

        writer.println('"');

        writer.print("SRC_URI=\"");
        writer.print(improveSrcUri(
                replaceWithVars(config.getDownloadUri().toString(), config)));
        if (config.hasBinjarUri()) {
            writer.print("\n\t" + improveBinjarUri(
                    replaceWithVars(config.getBinjarUri().toString(), config)));
        }
        if (config.hasTestSrcUri()) {
            writer.print("\n\t" + improveTestSrcUri(
                    replaceWithVars(config.getTestSrcUri().toString(), config)));
        }
        writer.println('"');

        writer.print("LICENSE=\"");

        if (config.getLicense() != null) {
            writer.print(config.getLicense());
        } else {
            writer.print(mavenProject.getLicenses());
        }

        writer.println('"');

        writer.print("SLOT=\"");
        writer.print(config.getSlot());
        writer.println('"');

        writer.print("KEYWORDS=\"");
        writer.print(config.getKeywords());
        writer.println('"');
    }

    /**
     * Writes ebuild script.
     *
     * @param config        application configuration
     * @param mavenProjects list of maven projects
     * @param writer        ebuild writer
     */
    private void writeScript(final Config config,
            final List<MavenProject> mavenProjects,
            final PrintWriter writer) {
        if (mavenProjects.size() == 1) {
            writeSingleProjectScript(config, mavenProjects.get(0), writer);
        } else {
            writeMultipleProjectsScript(config, mavenProjects, writer);
        }
    }

    /**
     * Writes ebuild script for single project.
     *
     * @param config       application configuration
     * @param mavenProject maven project
     * @param writer       ebuild writer
     */
    private void writeSingleProjectScript(final Config config,
            final MavenProject mavenProject, final PrintWriter writer) {
        writer.println();

        if (!"UTF-8".equals(mavenProject.getSourceEncoding())) {
            writer.print("JAVA_ENCODING=\"");
            writer.print(mavenProject.getSourceEncoding());
            writer.println("\"\n");
        }

        if (!mavenProject.getCommonDependencies().isEmpty()
                || !mavenProject.getRuntimeDependencies().isEmpty()) {
            final List<MavenDependency> dependencies
                    = new ArrayList<>(
                            mavenProject.getCommonDependencies().size()
                            + mavenProject.getRuntimeDependencies().size());
            dependencies.addAll(mavenProject.getCommonDependencies());
            dependencies.addAll(mavenProject.getRuntimeDependencies());

            writer.print("JAVA_GENTOO_CLASSPATH=\"");
            writer.print(createClassPath(dependencies));
            writer.println('"');
        }

        if (!mavenProject.getCompileDependencies().isEmpty()) {
            writer.print("JAVA_CLASSPATH_EXTRA=\"");
            writer.print(createClassPath(mavenProject.getCompileDependencies()));
            writer.println('"');
        }

        writer.print(mavenProject.getExtraJars(config.getStdoutWriter()));

        writer.print("JAVA_SRC_DIR=\"");
        writer.print(replaceWithVars(config.getWorkdir().relativize(
                mavenProject.getSourceDirectory()).toString(), config));
        writer.println('"');

        if (mavenProject.getMainClass() != null) {
            writer.print("JAVA_MAIN_CLASS=\"");
            writer.print(mavenProject.getMainClass());
            writer.println('"');
        }

        if (mavenProject.hasResources()) {
            writer.println("JAVA_RESOURCE_DIRS=(");

            mavenProject.getResourceDirectories().forEach((directory) -> {
                writer.print("\t\"");
                writer.print(replaceWithVars(
                        config.getWorkdir().relativize(directory).toString(),
                        config));
                writer.println('"');
            });

            writer.println(')');
        }

        if (config.hasBinjarUri()) {
            writer.println("JAVA_BINJAR_FILENAME=\"${P}-bin.jar\"");
        }

        boolean firstTestVar = true;

        if (!mavenProject.getTestDependencies().isEmpty()) {
            if (firstTestVar) {
                writer.println();
                firstTestVar = false;
            }

            writer.print("JAVA_TEST_GENTOO_CLASSPATH=\"");
            writer.print(createClassPath(mavenProject.getTestDependencies()));
            writer.println('"');
        }

        if (mavenProject.hasTests()) {
            if (firstTestVar) {
                writer.println();
            }

            writer.print("JAVA_TEST_SRC_DIR=\"");
            writer.print(replaceWithVars(config.getWorkdir().relativize(
                    mavenProject.getTestSourceDirectory()).toString(), config));
            writer.println('"');

            if (mavenProject.hasTestResources()) {
                writer.println("JAVA_TEST_RESOURCE_DIRS=(");

                mavenProject.getTestResourceDirectories().
                        forEach((directory) -> {
                            writer.print("\t\"");
                            writer.print(replaceWithVars(config.getWorkdir().
                                    relativize(directory).toString(), config));
                            writer.println('"');
                        });

                writer.println(')');
            }
        }
    }

    /**
     * Writes S directory.
     *
     * @param writer ebuild writer
     */
    private void writeSourceDir(final PrintWriter writer) {
        writer.println();
        writer.println("S=\"${WORKDIR}\"");
    }
}
