package org.gentoo.java.ebuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.gentoo.java.ebuilder.maven.JavaVersion;
import org.gentoo.java.ebuilder.maven.MavenCache;
import org.gentoo.java.ebuilder.maven.MavenEbuilder;
import org.gentoo.java.ebuilder.maven.MavenParser;
import org.gentoo.java.ebuilder.maven.MavenProject;
import org.gentoo.java.ebuilder.portage.PortageParser;

/**
 * Main class.
 *
 * @author fordfrog
 */
public class Main {

    /**
     * Main method for launching the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        final Config config = new Config(new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));

        if (args == null || args.length == 0) {
            printUsage(config);
            Runtime.getRuntime().exit(1);
        }

        parseArgs(config, args);
        checkArgs(config);

        if (config.isRefreshCache()) {
            refreshCache(config);
        }

        if (config.isGenerateEbuild()) {
            generateEbuild(config);
        }

        config.getStdoutWriter().println("Finished!");
        config.getStdoutWriter().flush();
        config.getErrorWriter().flush();
    }

    /**
     * Checks whether correct arguments are passed.
     *
     * @param config application configuration
     */
    private static void checkArgs(final Config config) {
        if (config.isRefreshCache()) {
            if (config.getPortageTree().isEmpty()) {
                config.addPortageTree(Paths.get("/usr/portage"));
            }

            for (Path portageTree : config.getPortageTree()) {
                if (!portageTree.toFile().exists()) {
                    config.getErrorWriter().println("ERROR: Portage tree "
                            + portageTree + " does not exist.");
                    Runtime.getRuntime().exit(1);
                }
            }
        } else if (!config.getPortageTree().isEmpty()) {
            config.getErrorWriter().println("WARNING: Portage tree is used "
                    + "only when refreshing cache.");
        }

        if (config.isGenerateEbuild()) {
            if (config.getDownloadUri() == null) {
                config.getErrorWriter().println(
                        "ERROR: --download-uri must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (config.getEbuild() == null) {
                config.getErrorWriter().println(
                        "ERROR: --ebuild must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (!config.getEbuild().getParent().toFile().exists()) {
                config.getErrorWriter().println("ERROR: Ebuild parent "
                        + "directory " + config.getEbuild().getParent()
                        + " does not exist.");
                Runtime.getRuntime().exit(1);
            } else if (config.getKeywords() == null) {
                config.getErrorWriter().println(
                        "ERROR: --keywords must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (config.getWorkdir() == null) {
                config.getErrorWriter().println(
                        "ERROR: --workdir must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (!config.getWorkdir().toFile().exists()) {
                config.getErrorWriter().println("ERROR: Workdir "
                        + config.getWorkdir().toFile().getPath()
                        + " does not exist.");
                Runtime.getRuntime().exit(1);
            } else if (config.getPomFiles().isEmpty()) {
                config.getErrorWriter().println(
                        "ERROR: --pom must be specified at least once.");
                Runtime.getRuntime().exit(1);
            }

            config.getPomFiles().stream().forEach((pomFile) -> {
                final File fullPath
                        = config.getWorkdir().resolve(pomFile).toFile();

                if (!fullPath.exists()) {
                    config.getErrorWriter().println("ERROR: POM file "
                            + fullPath + " does not exist.");
                    Runtime.getRuntime().exit(1);
                }
            });

            if (config.getSlot() == null) {
                config.setSlot("0");
            }
        } else if (config.getDownloadUri() != null) {
            config.getErrorWriter().println("WARNING: Download URI is used "
                    + "only when generating ebuild.");
        } else if (config.isDumpProjects()) {
            config.getErrorWriter().println("WARNING: Dumping of projects can "
                    + "be used only when generating ebuild.");
        } else if (config.getEbuild() != null) {
            config.getErrorWriter().println(
                    "WARNING: Ebuild is used only when generating ebuild.");
        } else if (config.getForceMinJavaVersion() != null) {
            config.getErrorWriter().println("WARNING: Forcing minimum JDK/JRE "
                    + "version applies only when generating ebuild.");
        } else if (config.getKeywords() != null) {
            config.getErrorWriter().println("WARNING: Keywords are used only "
                    + "when generating ebuild.");
        } else if (config.getLicense() != null) {
            config.getErrorWriter().println("WARNING: License is used only "
                    + "when generating ebuild.");
        } else if (!config.getPomFiles().isEmpty()) {
            config.getErrorWriter().println("WARNING: pom.xml is used only "
                    + "when generating ebuild.");
        } else if (config.getSlot() != null) {
            config.getErrorWriter().println("WARNING: SLOT is used only when "
                    + "generating ebuild.");
        } else if (config.getWorkdir() != null) {
            config.getErrorWriter().println("WARNING: Workdir is used only "
                    + "when generating ebuild.");
        }

        if (!config.isRefreshCache()
                && !config.getCacheFile().toFile().exists()) {
            config.getErrorWriter().println("ERROR: Cache file does not exist. "
                    + "First you must generate it using --refresh-cache.");
            Runtime.getRuntime().exit(1);
        }
    }

    private static void dumpMavenProjects(final Config config,
            final List<MavenProject> mavenProjects) {
        int i = 0;

        for (final MavenProject mavenProject : mavenProjects) {
            config.getStdoutWriter().println(MessageFormat.format(
                    "\n===== PROJECT {0} DUMP START =====", i));
            mavenProject.dump(config.getStdoutWriter());
            config.getStdoutWriter().println(MessageFormat.format(
                    "===== PROJECT {0} DUMP END =====", i));

            i++;
        }

        config.getStdoutWriter().println();
    }

    /**
     * Processed generation of ebuild.
     *
     * @param config application configuration
     */
    private static void generateEbuild(final Config config) {
        parseEbuildName(config);

        final MavenCache mavenCache = new MavenCache();
        mavenCache.loadCache(config);

        final MavenParser mavenParser = new MavenParser();
        final List<MavenProject> mavenProjects
                = mavenParser.parsePomFiles(config, mavenCache);

        if (config.isDumpProjects()) {
            dumpMavenProjects(config, mavenProjects);
        }

        final MavenEbuilder mavenEbuilder = new MavenEbuilder();
        mavenEbuilder.generateEbuild(config, mavenProjects, mavenCache);
    }

    /**
     * Parses command line arguments.
     *
     * @param config application configuration container
     * @param args   command line arguments
     */
    @SuppressWarnings("AssignmentToForLoopParameter")
    private static void parseArgs(final Config config, final String[] args) {
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];

            switch (arg) {
                case "--binjar-uri":
                    i++;

                    try {
                        config.setBinjarUri(new URI(args[i]));
                    } catch (final URISyntaxException ex) {
                        config.getErrorWriter().println(
                                "ERROR: URI goes to binary jar "
                                + args[i]
                                + " is not valid.");
                    }

                    break;
                case "--download-uri":
                case "-u":
                    i++;

                    try {
                        config.setDownloadUri(new URI(args[i]));
                    } catch (final URISyntaxException ex) {
                        config.getErrorWriter().println("ERROR: SRC_URI " + args[i]
                                + " is not valid.");
                    }

                    break;
                case "--dump-projects":
                case "-d":
                    config.setDumpProjects(true);
                    break;
                case "--ebuild":
                case "-e":
                    i++;
                    config.setEbuild(Paths.get(args[i]).toAbsolutePath().
                            normalize());
                    break;
                case "--force-min-java-version":
                    i++;
                    config.setForceMinJavaVersion(new JavaVersion(args[i]));
                    break;
                case "--from-maven-central":
                    config.setFromMavenCentral(true);
                    break;
                case "--generate-ebuild":
                case "-g":
                    config.setGenerateEbuild(true);
                    break;
                case "--keywords":
                case "-k":
                    i++;
                    config.addKeywords(args[i]);
                    break;
                case "--license":
                case "-l":
                    i++;
                    config.setLicense(args[i]);
                    break;
                case "--pom":
                case "-p":
                    i++;
                    config.addPomFile(Paths.get(args[i]));
                    break;
                case "--portage-tree":
                case "-t":
                    i++;
                    config.addPortageTree(Paths.get(args[i]).toAbsolutePath().
                            normalize());
                    break;
                case "--cache-file":
                    i++;
                    config.setCacheFile(Paths.get(args[i]).toAbsolutePath().
                            normalize());
                    break;
                case "--refresh-cache":
                case "-c":
                    config.setRefreshCache(true);
                    break;
                case "--skip-tests":
                    config.setSkipTests(true);
                    break;
                case "--slot":
                case "-s":
                    i++;
                    config.setSlot(args[i]);
                    break;
                case "--test-src-uri":
                    i++;

                    try {
                        config.setTestSrcUri(new URI(args[i]));
                    } catch (final URISyntaxException ex) {
                        config.getErrorWriter().println(
                                "ERROR: URI that goes to src code for testing"
                                + args[i]
                                + " is not valid.");
                    }

                    break;
                case "--workdir":
                case "-w":
                    i++;
                    config.setWorkdir(Paths.get(args[i]).toAbsolutePath().
                            normalize());
                    break;
                default:
                    config.getErrorWriter().println("ERROR: Switch '" + args[i]
                            + "' is not supported.");
                    Runtime.getRuntime().exit(1);
            }
        }
    }

    /**
     * Parses ebuild file name into its components.
     *
     * @param config app configuration containing ebuild information
     */
    private static void parseEbuildName(final Config config) {
        final Map<String, String> result;

        try {
            result = PortageParser.parseEbuildName(
                    config.getEbuild().getFileName().toString());

            config.setEbuildName(result.get("name"));
            config.setEbuildVersion(result.get("version"));
            config.setEbuildVersionSuffix(result.get("suffix"));

            config.getStdoutWriter().println(MessageFormat.format("Parsed "
                    + "ebuild file name: name = {0}, version = {1}, "
                    + "suffix = {2}",
                    config.getEbuildName(), config.getEbuildVersion(),
                    config.getEbuildVersionSuffix()));
        } catch (final IllegalArgumentException ex) {
            config.getErrorWriter().println("Cannot parse ebuild file name");

            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Prints application usage information.
     */
    private static void printUsage(final Config config) {
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(Main.class.getResourceAsStream(
                        "/usage.txt")))) {
            reader.lines().forEach((String line) -> {
                config.getStdoutWriter().println(line);
            });
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to read usage from resource", ex);
        }
    }

    /**
     * Processes cache refresh.
     *
     * @param config application configuration
     */
    private static void refreshCache(final Config config) {
        final PortageParser portageParser = new PortageParser();
        portageParser.parseTree(config);
    }
}
