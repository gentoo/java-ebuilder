package org.gentoo.java.ebuilder.maven;

import java.io.PrintWriter;
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
    private final List<MavenDependency> dependencies = new ArrayList<>(10);
    /**
     * Project description.
     */
    private String description;
    /**
     * Maven group id.
     */
    private String groupId;
    /**
     * Whether the package has resources.
     */
    private Boolean hasResources;
    /**
     * Whether the package has test resources.
     */
    private Boolean hasTestResources;
    /**
     * Whether the package has test classes.
     */
    private Boolean hasTests;
    /**
     * Lisences.
     */
    private List<String> licenses = new ArrayList<>(10);
    /**
     * Homepage URL.
     */
    private String homepage;
    /**
     * Application main class.
     */
    private String mainClass;
    /**
     * Path to pom.xml file.
     */
    private final Path pomFile;
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
    private String sourceEncoding = "UTF-8";
    /**
     * Source compile version.
     */
    private JavaVersion sourceVersion = new JavaVersion("1.8");
    /**
     * Target compile version.
     */
    private JavaVersion targetVersion = new JavaVersion("1.8");
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
     * Creates new instance of MavenProject.
     *
     * @param pomFile {@link #pomFile}
     */
    public MavenProject(final Path pomFile) {
        this.pomFile = pomFile;
    }

    /**
     * Adds dependency to {@link #dependencies}.
     *
     * @param dependency {@link #dependencies}
     */
    public void addDependency(final MavenDependency dependency) {
        dependencies.add(dependency);
    }

    /**
     * Adds license to {@link #licenses}.
     *
     * @param license {@link #licenses}
     */
    public void addLicense(final String license) {
        licenses.add(license);
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
     * Outputs project properties to the writer.
     *
     * @param writer writer
     */
    public void dump(final PrintWriter writer) {
        writer.print("POM file: ");
        writer.println(pomFile);
        writer.print("groupId: ");
        writer.println(groupId);
        writer.print("artifactId: ");
        writer.println(artifactId);
        writer.print("version: ");
        writer.println(version);
        writer.print("description: ");
        writer.println(description);
        writer.print("homepage: ");
        writer.println(homepage);
        writer.print("mainClass: ");
        writer.println(mainClass);
        writer.print("sourceVersion: ");
        writer.println(sourceVersion);
        writer.print("targetVersion: ");
        writer.println(targetVersion);
        writer.print("sourceEncoding: ");
        writer.println(sourceEncoding);
        writer.print("sourceDirectory: ");
        writer.println(sourceDirectory);
        writer.print("hasResources: ");
        writer.println(hasResources());

        writer.println("resourceDirectories:");

        if (resourceDirectories != null) {
            resourceDirectories.forEach((resourceDirectory) -> {
                writer.print("  ");
                writer.println(resourceDirectory);
            });
        }

        writer.print("hasTests: ");
        writer.println(hasTests());
        writer.print("testSourceDirectory: ");
        writer.println(testSourceDirectory);
        writer.print("hasTestResources: ");
        writer.println(hasTestResources());

        writer.println("testResourceDirectories:");

        testResourceDirectories.forEach((testResourceDirectory) -> {
            writer.print("  ");
            writer.println(testResourceDirectory);
        });

        writer.println("dependencies:");

        if (dependencies != null) {
            dependencies.forEach((dependency) -> {
                writer.print("- groupId: ");
                writer.println(dependency.getGroupId());
                writer.print("  artifactId: ");
                writer.println(dependency.getArtifactId());
                writer.print("  mavenVersion: ");
                writer.println(dependency.getMavenVersion().getVersion());
                writer.print("  scope: ");
                writer.println(dependency.getScope());
                writer.print("  systemDependency: ");
                writer.println(dependency.getSystemDependency());
                writer.print("  version: ");
                writer.println(dependency.getVersion());
            });
        }
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
     * Returns list of dependencies that are used both for compilation and
     * runtime.
     *
     * @return list of dependencies
     */
    public List<MavenDependency> getCommonDependencies() {
        return getDependencies(new String[]{"compile"});
    }

    /**
     * Returns list of dependencies that are needed only for compilation.
     *
     * @return list of dependencies
     */
    public List<MavenDependency> getCompileDependencies() {
        return getDependencies(new String[]{"provided"});
    }

    /**
     * Getter for {@link #dependencies}. The list is read-only.
     *
     * @return {@link #dependencies}
     */
    public List<MavenDependency> getDependencies() {
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
     * Getter for {@link #licenses}.
     *
     * @return space separated licenses
     */
    public String getLicenses() {
        return String.join(" ", licenses);
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
     * Getter for {@link #pomFile}.
     *
     * @return {@link #pomFile}
     */
    public Path getPomFile() {
        return pomFile;
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
     * Returns list of dependencies that are needed only for runtime.
     *
     * @return list of dependencies
     */
    public List<MavenDependency> getRuntimeDependencies() {
        return getDependencies(new String[]{"runtime"});
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
    public JavaVersion getSourceVersion() {
        return sourceVersion;
    }

    /**
     * Setter for {@link #sourceVersion}.
     *
     * @param sourceVersion {@link #sourceVersion}
     */
    public void setSourceVersion(final JavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    /**
     * Getter for {@link #targetVersion}.
     *
     * @return {@link #targetVersion}
     */
    public JavaVersion getTargetVersion() {
        return targetVersion;
    }

    /**
     * Setter for {@link #targetVersion}.
     *
     * @param targetVersion {@link #targetVersion}
     */
    public void setTargetVersion(final JavaVersion targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
     * Returns list of dependencies that are needed only for tests compilation
     * and runtime.
     *
     * @return list of dependencies
     */
    @SuppressWarnings("unchecked")
    public List<MavenDependency> getTestDependencies() {
        if (!hasTests()) {
            return Collections.EMPTY_LIST;
        }

        return getDependencies(new String[]{"test"});
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
     * Getter for {@link #hasResources}.
     *
     * @return {@link #hasResources}
     */
    public boolean hasResources() {
        if (hasResources == null) {
            hasResources = false;

            for (final Path resources : resourceDirectories) {
                if (resources.toFile().exists()
                        && resources.toFile().list().length != 0) {
                    hasResources = true;

                    break;
                }
            }
        }

        return hasResources;
    }

    /**
     * Getter for {@link #hasTestResources}.
     *
     * @return {@link #hasTestResources}
     */
    public boolean hasTestResources() {
        if (hasTestResources == null) {
            hasTestResources = false;

            for (final Path resources : testResourceDirectories) {
                if (resources.toFile().exists()
                        && resources.toFile().list().length != 0) {
                    hasTestResources = true;

                    break;
                }
            }
        }

        return hasTestResources;
    }

    /**
     * Getter for {@link #hasTests}.
     *
     * @return {@link #hasTests}
     */
    public boolean hasTests() {
        if (hasTests == null) {
            hasTests = testSourceDirectory != null
                    && testSourceDirectory.toFile().exists()
                    && testSourceDirectory.toFile().list().length != 0;
        }

        return hasTests;
    }

    /**
     * Returns dependencies based on the specified scopes.
     *
     * @param scopes array of scopes
     *
     * @return list of dependencies
     */
    private List<MavenDependency> getDependencies(final String[] scopes) {
        final List<MavenDependency> result
                = new ArrayList<>(dependencies.size());

        for (final MavenDependency dependency : dependencies) {
            for (final String scope : scopes) {
                if (dependency.getScope().equals(scope)) {
                    result.add(dependency);

                    break;
                }
            }
        }

        result.sort((final MavenDependency o1, final MavenDependency o2) -> {
            if (!o1.getGroupId().equals(o2.getGroupId())) {
                return o1.getGroupId().compareTo(o2.getGroupId());
            } else {
                return o1.getArtifactId().compareTo(o2.getArtifactId());
            }
        });

        return result;
    }
}
