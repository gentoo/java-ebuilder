package org.gentoo.java.ebuilder.maven;

import org.gentoo.java.ebuilder.portage.MavenVersion;

/**
 * Maven project dependency.
 *
 * @author fordfrog
 */
public class MavenDependency {

    /**
     * Artifact id.
     */
    private final String artifactId;
    /**
     * Group id.
     */
    private final String groupId;
    /**
     * Parsed maven version.
     */
    private final MavenVersion mavenVersion;
    /**
     * Dependency scope.
     */
    private final String scope;
    /**
     * System dependency.
     */
    private final String systemDependency;
    /**
     * Version.
     */
    private final String version;

    /**
     * Creates new instance of Dependency.
     *
     * @param groupId          {@link #groupId}.
     * @param artifactId       {@link #artifactId}
     * @param version          {@link #version}
     * @param scope            {@link #scope}
     * @param systemDependency {@link #systemDependency}
     */
    public MavenDependency(final String groupId, final String artifactId,
            final String version, final String scope,
            final String systemDependency) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.systemDependency = systemDependency;
        mavenVersion = new MavenVersion(version);
    }

    /**
     * Getter for {@link #artifactId}.
     *
     * @return {@link #artifactId}
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Getter for {@link #groupId}.
     *
     * @return {@link #groupId}
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Getter for {@link #mavenVersion}.
     *
     * @return {@link #mavenVersion}
     */
    public MavenVersion getMavenVersion() {
        return mavenVersion;
    }

    /**
     * Getter for {@link #scope}.
     *
     * @return {@link #scope}
     */
    public String getScope() {
        return scope;
    }

    /**
     * Getter for {@link #systemDependency}.
     *
     * @return {@link #systemDependency}
     */
    public String getSystemDependency() {
        return systemDependency;
    }

    /**
     * Getter for {@link #version}.
     *
     * @return {@link #version}
     */
    public String getVersion() {
        return version;
    }

}
