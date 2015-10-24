package org.gentoo.java.ebuilder.maven;

import org.gentoo.java.ebuilder.Config;

/**
 * Generates ebuild from maven project.
 *
 * @author fordfrog
 */
public class MavenEbuilder {

    /**
     * Generates ebuild from the collected information at the specified path.
     *
     * @param config       application configuration
     * @param mavenProject maven project information
     */
    public void generateEbuild(final Config config, MavenProject mavenProject) {
        // TODO
        config.getStdoutWriter().println(
                "WARNING: Generating ebuild is not implemented yet.");
    }
}
