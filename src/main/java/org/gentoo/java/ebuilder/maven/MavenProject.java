package org.gentoo.java.ebuilder.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains information about maven project collected from pom.xml.
 *
 * @author fordfrog
 */
public class MavenProject {

    /**
     * Maven artifact id.
     */
    private String artifactId;
    /**
     * List of project dependencies.
     */
    private final List<Dependency> dependencies = new ArrayList<>(10);
    /**
     * Project description.
     */
    private String description;
    /**
     * Maven group id.
     */
    private String groupId;
    /**
     * Homepage URL.
     */
    private String homepage;
    /**
     * Application main class.
     */
    private String mainClass;
    /**
     * List of resource directories.
     */
    private final List<Path> resourceDirectories = new ArrayList<>(10);
    /**
     * Source directory.
     */
    private Path sourceDirectory;
    /**
     * Source encoding.
     */
    private String sourceEncoding = "1.7";
    /**
     * Source compile version.
     */
    private String sourceVersion = "1.7";
    /**
     * Target compile version.
     */
    private String targetVersion;
    /**
     * Test resource directories.
     */
    private final List<Path> testResourceDirectories = new ArrayList<>(10);
    /**
     * Test source directory.
     */
    private Path testSourceDirectory;
    /**
     * Maven version.
     */
    private String version;

    /**
     * Adds dependency to {@link #dependencies}.
     *
     * @param dependency {@link #dependencies}
     */
    public void addDependency(final Dependency dependency) {
        dependencies.add(dependency);
    }

    /**
     * Adds path to {@link #resourceDirectories}.
     *
     * @param path resource path
     */
    public void addResourceDirectory(final Path path) {
        resourceDirectories.add(path);
    }

    /**
     * Adds path to {@link #testResourceDirectories}.
     *
     * @param path resource path
     */
    public void addTestResourceDirectory(final Path path) {
        testResourceDirectories.add(path);
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
     * Setter for {@link #artifactId}.
     *
     * @param artifactId {@link #artifactId}
     */
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Getter for {@link #dependencies}. The list is read-only.
     *
     * @return {@link #dependencies}
     */
    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Getter for {@link #description}.
     *
     * @return {@link #description}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for {@link #description}.
     *
     * @param description {@link #description}
     */
    public void setDescription(final String description) {
        this.description = description;
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
     * Setter for {@link #groupId}.
     *
     * @param groupId {@link #groupId}
     */
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    /**
     * Getter for {@link #homepage}.
     *
     * @return {@link #homepage}
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Setter for {@link #homepage}.
     *
     * @param homepage {@link #homepage}
     */
    public void setHomepage(final String homepage) {
        this.homepage = homepage;
    }

    /**
     * Getter for {@link #mainClass}.
     *
     * @return {@link #mainClass}
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Setter for {@link #mainClass}.
     *
     * @param mainClass {@link #mainClass}
     */
    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * Getter for {@link #resourceDirectories}. The list is read-only.
     *
     * @return {@link #resourceDirectories}
     */
    public List<Path> getResourceDirectories() {
        return Collections.unmodifiableList(resourceDirectories);
    }

    /**
     * Getter for {@link #sourceDirectory}.
     *
     * @return {@link #sourceDirectory}
     */
    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Setter for {@link #sourceDirectory}.
     *
     * @param sourceDirectory {@link #sourceDirectory}
     */
    public void setSourceDirectory(final Path sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * Getter for {@link #sourceEncoding}.
     *
     * @return {@link #sourceEncoding}
     */
    public String getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Setter for {@link #sourceEncoding}.
     *
     * @param sourceEncoding {@link #sourceEncoding}
     */
    public void setSourceEncoding(final String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * Getter for {@link #sourceVersion}.
     *
     * @return {@link #sourceVersion}
     */
    public String getSourceVersion() {
        return sourceVersion;
    }

    /**
     * Setter for {@link #sourceVersion}.
     *
     * @param sourceVersion {@link #sourceVersion}
     */
    public void setSourceVersion(final String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    /**
     * Getter for {@link #targetVersion}.
     *
     * @return {@link #targetVersion}
     */
    public String getTargetVersion() {
        return targetVersion;
    }

    /**
     * Setter for {@link #targetVersion}.
     *
     * @param targetVersion {@link #targetVersion}
     */
    public void setTargetVersion(final String targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
     * Getter for {@link #testResourceDirectories}. The list is read-only.
     *
     * @return {@link #testResourceDirectories}
     */
    public List<Path> getTestResourceDirectories() {
        return Collections.unmodifiableList(testResourceDirectories);
    }

    /**
     * Getter for {@link #testSourceDirectory}.
     *
     * @return {@link #testSourceDirectory}
     */
    public Path getTestSourceDirectory() {
        return testSourceDirectory;
    }

    /**
     * Setter for {@link #testSourceDirectory}.
     *
     * @param testSourceDirectory {@link #testSourceDirectory}
     */
    public void setTestSourceDirectory(final Path testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    /**
     * Getter for {@link #version}.
     *
     * @return {@link #version}
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter for {@link #version}.
     *
     * @param version {@link #version}
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Maven project dependency.
     */
    @SuppressWarnings("PublicInnerClass")
    public static class Dependency {

        /**
         * Artifact id.
         */
        private final String artifactId;
        /**
         * Group id.
         */
        private final String groupId;
        /**
         * Dependency scope.
         */
        private final String scope;
        /**
         * Version.
         */
        private final String version;

        /**
         * Creates new instance of Dependency.
         *
         * @param groupId    {@link #groupId}.
         * @param artifactId {@link #artifactId}
         * @param version    {@link #version}
         * @param scope      {@link #scope}
         */
        public Dependency(final String groupId, final String artifactId,
                final String version, final String scope) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
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
         * Getter for {@link #scope}.
         *
         * @return {@link #scope}
         */
        public String getScope() {
            return scope;
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
}
