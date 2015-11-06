package org.gentoo.java.ebuilder.maven;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.gentoo.java.ebuilder.Config;

/**
 * Generates ebuild from maven project.
 *
 * @author fordfrog
 */
public class MavenEbuilder {

    /**
     * EAPI version.
     */
    private static final String EAPI = "5";

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
            writeInherit(writer);
            // write the info from the last project as it is probably the one
            // that depends on the rest
            writePackageInfo(config,
                    mavenProjects.get(mavenProjects.size() - 1), writer);

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

                    sbCP.append(dependency.getSystemDependency().
                            replaceAll(".*/", "").
                            replaceAll("\\[.*\\]", "").
                            replace(":", "-"));
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
            final List<MavenProject> mavenProjects) {
        for (final MavenProject mavenProject : mavenProjects) {
            final String result = determineTestingFramework(mavenProject);

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
    private String determineTestingFramework(final MavenProject mavenProject) {
        for (final MavenDependency dependency : mavenProject.
                getTestDependencies()) {
            if ("junit".equals(dependency.getGroupId())
                    && "junit".equals(dependency.getArtifactId())) {
                return "junit";
            }
        }

        for (final MavenDependency dependency : mavenProject.
                getCommonDependencies()) {
            if ("junit".equals(dependency.getGroupId())
                    && "junit".equals(dependency.getArtifactId())) {
                return "junit";
            }
        }

        return null;
    }

    /**
     * Retrieves minimum source version from the maven projects.
     *
     * @param mavenProjects list of maven projects
     *
     * @return minimum source version
     */
    private String getMinSourceVersion(final List<MavenProject> mavenProjects) {
        String result = null;

        for (final MavenProject mavenProject : mavenProjects) {
            if (result == null || mavenProject.getSourceVersion().compareTo(
                    result) < 0) {
                result = mavenProject.getSourceVersion();
            }
        }

        return result;
    }

    /**
     * Retrieves minimum target version from the maven projects.
     *
     * @param mavenProjects list of maven projects
     *
     * @return minimum target version
     */
    private String getMinTargetVersion(final List<MavenProject> mavenProjects) {
        String result = null;

        for (final MavenProject mavenProject : mavenProjects) {
            if (result == null || mavenProject.getTargetVersion().compareTo(
                    result) < 0) {
                result = mavenProject.getTargetVersion();
            }
        }

        return result;
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
                    && !result.contains(dependency.getSystemDependency()))).
                    forEach((dependency) -> {
                        result.add(dependency.getSystemDependency());
                    });
        });

        result.sort((final String o1, final String o2) -> {
            return o1.compareTo(o2);
        });

        return result;
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

        if (config.getDownloadUri() != null) {
            writer.print(" --download-uri ");
            writer.print(config.getDownloadUri());
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
        boolean hasCDepend = !commonDependencies.isEmpty()
                || !testDependencies.isEmpty();

        if (hasCDepend) {
            writer.println();
            writer.println("# Common dependencies");

            for (final MavenProject mavenProject : mavenProjects) {
                if (mavenProject.getCommonDependencies().isEmpty()
                        && mavenProject.getTestDependencies().isEmpty()) {
                    continue;
                }

                if (!mavenProject.getCommonDependencies().isEmpty()) {
                    writeDependenciesInfo(writer, mavenProject.getPomFile(),
                            mavenProject.getCommonDependencies(), null);
                }

                if (!mavenProject.getTargetVersion().isEmpty()) {
                    writeDependenciesInfo(writer, mavenProject.getPomFile(),
                            mavenProject.getTestDependencies(), "test?");
                }

                hasCDepend = true;
            }

            writer.print("CDEPEND=\"");

            if (!commonDependencies.isEmpty()) {
                commonDependencies.stream().
                        forEach((dependency) -> {
                            writer.println();
                            writer.print('\t');
                            writer.print(dependency);
                        });
            }

            if (!testDependencies.isEmpty()) {
                writer.println();
                writer.println("\ttest? (");

                testDependencies.stream().
                        forEach((dependency) -> {
                            writer.println();
                            writer.print("\t\t");
                            writer.print(dependency);
                        });

                writer.print("\t)");
            }

            writer.println('"');
        }

        if (!compileDependencies.isEmpty()) {
            writer.println("# Compile dependencies");

            mavenProjects.stream().filter((mavenProject) -> (!mavenProject.
                    getCompileDependencies().isEmpty()))
                    .forEach((mavenProject) -> {
                        writeDependenciesInfo(writer, mavenProject.getPomFile(),
                                mavenProject.getCompileDependencies(), null);
                    });
        } else {
            writer.println();
        }

        writer.print("DEPEND=\"");

        if (hasCDepend) {
            writer.print("${CDEPEND}");
        }

        writer.println();
        writer.print("\t>=virtual/jdk-");
        writer.print(getMinSourceVersion(mavenProjects));

        if (config.getDownloadUri() != null && config.getDownloadUri().
                toString().matches("^.*?\\.(jar|zip)$")) {
            writer.println();
            writer.print("\tapp-arch/unzip");
        }

        if (!compileDependencies.isEmpty()) {
            compileDependencies.stream().
                    forEach((dependency) -> {
                        writer.println();
                        writer.print('\t');
                        writer.print(dependency);
                    });
        }

        writer.println('"');

        if (!runtimeDependencies.isEmpty()) {
            writer.println("# Runtime dependencies");

            mavenProjects.stream().filter((mavenProject) -> (!mavenProject.
                    getRuntimeDependencies().isEmpty()))
                    .forEach((mavenProject) -> {
                        writeDependenciesInfo(writer, mavenProject.getPomFile(),
                                mavenProject.getRuntimeDependencies(), null);
                    });
        } else {
            writer.println();
        }

        writer.print("RDEPEND=\"");

        if (hasCDepend) {
            writer.print("${CDEPEND}");
        }

        writer.println();
        writer.print("\t>=virtual/jre-");
        writer.print(getMinTargetVersion(mavenProjects));

        if (!runtimeDependencies.isEmpty()) {
            runtimeDependencies.stream().
                    forEach((dependency) -> {
                        writer.println();
                        writer.print('\t');
                        writer.print(dependency);
                    });
        }

        writer.println('"');
    }

    /**
     * Writes dependencies information to the ebuild.
     *
     * @param writer       ebuild writer
     * @param pomFile      path to pom file
     * @param dependencies list of dependencies
     * @param useFlag      optional USE flag including question mark
     */
    private void writeDependenciesInfo(final PrintWriter writer,
            final Path pomFile, final List<MavenDependency> dependencies,
            final String useFlag) {
        writer.print("# POM: ");
        writer.println(pomFile);

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
        writer.printf("# Copyright 1999-%d Gentoo Foundation\n",
                LocalDate.now().getYear());
        writer.println("# Distributed under the terms of the GNU General "
                + "Public License v2");
        writer.println("# $Id$");
    }

    /**
     * Writes inherit line.
     *
     * @param writer ebuild writer
     */
    private void writeInherit(final PrintWriter writer) {
        writer.println();
        writer.println("inherit java-pkg-2 java-pkg-simple");
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
        }

        writer.println('"');

        writer.print("HOMEPAGE=\"");

        if (mavenProject.getHomepage() != null) {
            writer.print(mavenProject.getHomepage());
        }

        writer.println('"');

        writer.print("SRC_URI=\"");
        writer.print(config.getDownloadUri());
        writer.println('"');

        writer.print("LICENSE=\"");

        if (config.getLicense() != null) {
            writer.print(config.getLicense());
        }

        writer.println('"');

        writer.print("SLOT=\"");
        writer.print(config.getSlot());
        writer.println('"');

        writer.print("KEYWORDS=\"");
        writer.print(config.getKeywords());
        writer.println('"');

        writer.print("IUSE=\"doc source");

        if (mavenProject.hasTests()) {
            writer.print(" test");
        }

        writer.println('"');

        writer.print("MAVEN_ID=\"");
        writer.print(mavenProject.getGroupId());
        writer.print(':');
        writer.print(mavenProject.getArtifactId());
        writer.print(':');
        writer.print(mavenProject.getVersion());
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

        if (!mavenProject.getTestDependencies().isEmpty()) {
            writer.print("JAVA_GENTOO_TEST_CLASSPATH=\"");
            writer.print(createClassPath(mavenProject.getTestDependencies()));
            writer.println('"');
        }

        final String testingFramework = determineTestingFramework(mavenProject);

        if (testingFramework != null) {
            writer.print("JAVA_TESTING_FRAMEWORK=\"");
            writer.print(testingFramework);
            writer.println('"');
        }

        writer.print("JAVA_SRC_DIR=\"");
        writer.print(config.getWorkdir().relativize(
                mavenProject.getSourceDirectory()));
        writer.println('"');

        if (mavenProject.hasResources()) {
            writer.print("JAVA_RESOURCE_DIRS=\"");

            boolean first = true;

            for (final Path resources : mavenProject.getResourceDirectories()) {
                if (first) {
                    first = false;
                } else {
                    writer.print(':');
                }

                writer.print(config.getWorkdir().relativize(resources));
            }

            writer.println('"');
        }

        if (mavenProject.hasTests()) {
            writer.print("JAVA_TEST_SRC_DIR=\"");
            writer.print(config.getWorkdir().relativize(
                    mavenProject.getTestSourceDirectory()));
            writer.println('"');

            if (mavenProject.hasTestResources()) {
                writer.print("JAVA_TEST_RESOURCE_DIRS=\"");

                boolean first = true;

                for (final Path resources : mavenProject.
                        getTestResourceDirectories()) {
                    if (first) {
                        first = false;
                    } else {
                        writer.print(':');
                    }

                    writer.print(config.getWorkdir().relativize(resources));
                }

                writer.println('"');
            }
        }

        if (!"UTF-8".equals(mavenProject.getSourceEncoding())) {
            writer.print("JAVA_ENCODING=\"");
            writer.print(mavenProject.getSourceEncoding());
            writer.println('"');
        }

        if (mavenProject.getMainClass() != null) {
            writer.print("JAVA_MAIN_CLASS=\"");
            writer.print(mavenProject.getMainClass());
            writer.println('"');
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
