package org.gentoo.java.ebuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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
            if (config.getPortageTree() == null) {
                config.setPortageTree(Paths.get("/usr/portage"));
            }

            if (!config.getPortageTree().toFile().exists()) {
                config.getErrorWriter().println("ERROR: Portage tree "
                        + config.getPortageTree() + " does not exist.");
                Runtime.getRuntime().exit(1);
            }
        } else if (config.getPortageTree() != null) {
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
            } else if (config.getTarballRoot() == null) {
                config.getErrorWriter().println(
                        "ERROR: --tarball-root must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (!config.getTarballRoot().toFile().exists()) {
                config.getErrorWriter().println("ERROR: Tarball root "
                        + config.getTarballRoot().toFile().getPath()
                        + " does not exist.");
                Runtime.getRuntime().exit(1);
            } else if (config.getPom() == null) {
                config.getErrorWriter().println(
                        "ERROR: --pom must be specified.");
                Runtime.getRuntime().exit(1);
            } else if (!config.getTarballRoot().resolve(config.getPom()).
                    toFile().exists()) {
                config.getErrorWriter().println("ERROR: POM file "
                        + config.getTarballRoot().resolve(config.getPom())
                        + " does not exist.");
                Runtime.getRuntime().exit(1);
            }
        } else if (config.getDownloadUri() != null) {
            config.getErrorWriter().println("WARNING: Download URI is used "
                    + "only when generating ebuild.");
        } else if (config.getEbuild() != null) {
            config.getErrorWriter().println(
                    "WARNING: Ebuild is used only when "
                    + "generating ebuild.");
        } else if (config.getPom() != null) {
            config.getErrorWriter().println("WARNING: pom.xml is used only "
                    + "when generating ebuild.");
        } else if (config.getTarballRoot() != null) {
            config.getErrorWriter().println("WARNING: Tarball root is used "
                    + "only when generating ebuild.");
        }

        if (!config.isRefreshCache()
                && !config.getCacheFile().toFile().exists()) {
            config.getErrorWriter().println("ERROR: Cache file does not exist. "
                    + "First you must generate it using --refresh-cache.");
            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Processed generation of ebuild.
     *
     * @param config application configuration
     */
    private static void generateEbuild(final Config config) {
        final MavenParser mavenParser = new MavenParser();
        final MavenProject mavenProject = mavenParser.parsePom(config);

        final MavenCache mavenCache = new MavenCache();
        mavenCache.loadCache(config.getCacheFile());

        final MavenEbuilder mavenEbuilder = new MavenEbuilder();
        mavenEbuilder.generateEbuild(config, mavenProject);
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
                case "--download-uri":
                case "-u":
                    i++;

                    try {
                        config.setDownloadUri(new URI(args[i]));
                    } catch (final URISyntaxException ex) {
                        config.getErrorWriter().println("ERROR: URI " + args[i]
                                + " is not valid.");
                    }

                    break;
                case "--ebuild":
                case "-e":
                    i++;
                    config.setEbuild(Paths.get(args[i]).toAbsolutePath());
                    break;
                case "--generate-ebuild":
                case "-g":
                    config.setGenerateEbuild(true);
                    break;
                case "--pom":
                case "-p":
                    i++;
                    config.setPom(Paths.get(args[i]));
                    break;
                case "-portage-tree":
                case "-t":
                    i++;
                    config.setPortageTree(Paths.get(args[i]).toAbsolutePath());
                    break;
                case "--refresh-cache":
                case "-c":
                    config.setRefreshCache(true);
                    break;
                case "--tarball-root":
                case "-r":
                    i++;
                    config.setTarballRoot(Paths.get(args[i]).toAbsolutePath());
                    break;
                default:
                    config.getErrorWriter().println("ERROR: Switch '" + args[i]
                            + "' is not supported.");
                    Runtime.getRuntime().exit(1);
            }
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
