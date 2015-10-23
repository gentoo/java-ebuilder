package org.gentoo.java.ebuilder;

import org.gentoo.java.ebuilder.maven.MavenParser;
import org.gentoo.java.ebuilder.maven.MavenProject;

/**
 * Main class.
 *
 * @author fordfrog
 */
public class Main {

    /**
     * Main method for launching the application.
     *
     * <p>
     * The application can be run in two modes.
     * </p>
     * <dl>
     * <dt>--refresh-cache</dt>
     * <dd>create/refresh cache of our ebuilds that provide jars</dd>
     * <dt>--generate-ebuild</dt>
     * <dd>Generates ebuild from pom.xml file</dd>
     * </dl>
     * <p>
     * Usage:
     * </p>
     * <pre>jar --refresh-db [path to tree]</pre>
     * <p>
     * It scans all ebuilds in [path to tree] (default is /usr/portage) and
     * creates/refreshes cache with ebuild information that is used for
     * generating ebuilds.
     * </p>
     * <pre>jar --generate-ebuild pom.xml package.ebuild</pre>
     * <p>
     * Parses pom.xml and create package.ebuild from the parsed information.
     * </p>
     *
     * @param args command line arguments
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(final String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            Runtime.getRuntime().exit(1);
        }

        switch (args[0]) {
            case "--generate-ebuild":
                generateEbuild(args);
                break;
            case "--refresh-cache":
                refreshCache(args);
                break;
            default:
                System.out.println("Unsupported switch: " + args[0]);
                Runtime.getRuntime().exit(1);
        }

        System.out.println("Done!");
    }

    /**
     * Processed generation of ebuild.
     *
     * @param args command line arguments
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void generateEbuild(String[] args) {
        if (args.length != 3) {
            System.out.println(
                    "Usage: jar --generate-ebuild pom.xml package.ebuild");
            Runtime.getRuntime().exit(1);
        }

        final String pomPath = args[1];
        final String ebuildPath = args[2];

        final MavenParser mavenParser = new MavenParser();
        final MavenProject mavenProject = mavenParser.parsePom(pomPath);
        mavenProject.generateEbuild(ebuildPath);
    }

    /**
     * Prints application usage information.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void printUsage() {
        System.out.println("Usage:\njar --refresh-db [path to tree]\n"
                + "jar --generate-ebuild pom.xml package.ebuild");
    }

    /**
     * Processes cache refresh.
     *
     * @param args command line arguments
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void refreshCache(String[] args) {
        if (args.length > 2) {
            System.out.println("Usage: jar --refresh-cache [path to tree]");
        }

        final String treePath;

        if (args.length == 1) {
            treePath = "/usr/portage";
        } else {
            treePath = args[1];
        }

        final PortageParser portageParser = new PortageParser();
        portageParser.parseTree(treePath);
    }
}
