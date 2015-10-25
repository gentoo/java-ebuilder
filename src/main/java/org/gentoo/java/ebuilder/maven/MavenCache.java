package org.gentoo.java.ebuilder.maven;

import java.nio.file.Path;

/**
 * Cache for resolving maven artifacts into portage ebuilds.
 *
 * @author fordfrog
 */
public class MavenCache {

    /**
     * Searches for system dependency using maven group id, artifact id and
     * version.
     *
     * @param groupId    maven group id
     * @param artifactId maven artifact id
     * @param version    maven version
     *
     * @return dependency string or null
     */
    public String getDependency(final String groupId, final String artifactId,
            final String version) {
        // TODO

        return null;
    }

    /**
     * Loads cache from specified path.
     *
     * @param cachePath cache path
     */
    public void loadCache(final Path cachePath) {
        // TODO
    }
}
