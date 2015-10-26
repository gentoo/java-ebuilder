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
 * TODO: add support for OSGi.
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
     * @param config       application configuration
     * @param mavenProject maven project information
     * @param mavenCache   populated maven cache
     */
    public void generateEbuild(final Config config, MavenProject mavenProject,
            final MavenCache mavenCache) {
        config.getStdoutWriter().print("Writing ebuild...");

        try (final PrintWriter writer = new PrintWriter(
                new FileWriter(config.getEbuild().toFile()))) {
            writeHeader(writer);
            writeCommand(config, writer);
            writeEAPI(writer);
            writeInherit(writer);
            writePackageInfo(config, mavenProject, writer);

            final List<ResolvedDependency> commonDependencies
                    = resolveDependencies(mavenProject.getCommonDependencies(),
                            mavenCache);
            final List<ResolvedDependency> testDependencies
                    = resolveDependencies(mavenProject.getTestDependencies(),
                            mavenCache);
            final List<ResolvedDependency> compileDependencies
                    = resolveDependencies(mavenProject.getCompileDependencies(),
                            mavenCache);
            final List<ResolvedDependency> runtimeDependencies
                    = resolveDependencies(mavenProject.getRuntimeDependencies(),
                            mavenCache);

            writeDependencies(mavenProject, commonDependencies,
                    testDependencies, compileDependencies, runtimeDependencies,
                    writer);
            writeSourceDir(writer);
            writeScript(config, mavenProject, commonDependencies,
                    testDependencies, compileDependencies, runtimeDependencies,
                    writer);
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
    private String createClassPath(final List<ResolvedDependency> dependencies) {
        final StringBuilder sbCP = new StringBuilder(dependencies.size() * 15);

        dependencies.stream().filter((final ResolvedDependency dependency)
                -> dependency.getSystemDependency() != null).
                forEach((final ResolvedDependency dependency) -> {
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
     * Attempts to resolve dependencies using the specified cache.
     *
     * @param dependencies list of maven dependencies
     * @param mavenCache   maven cache
     *
     * @return list of resolved dependencies (system dependency can be null)
     */
    private List<ResolvedDependency> resolveDependencies(
            final List<MavenProject.Dependency> dependencies,
            final MavenCache mavenCache) {
        final List<ResolvedDependency> result
                = new ArrayList<>(dependencies.size());

        dependencies.stream().forEach((dependency) -> {
            result.add(new ResolvedDependency(dependency,
                    mavenCache.getDependency(dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getVersion())));
        });

        return result;
    }

    /**
     * Sorts dependencies using system dependency.
     *
     * @param dependencies list of dependencies
     */
    private void sortDependencies(final List<ResolvedDependency> dependencies) {
        dependencies.sort((final ResolvedDependency o1,
                final ResolvedDependency o2) -> {
            if (o1.getSystemDependency() == null
                    && o2.getSystemDependency() == null) {
                return 0;
            } else if (o1.getSystemDependency() == null) {
                return 1;
            } else if (o2.getSystemDependency() == null) {
                return -1;
            } else {
                return o1.getSystemDependency().compareTo(
                        o2.getSystemDependency());
            }
        });
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

        if (config.getPom() != null) {
            writer.print(" --pom ");
            writer.print(config.getPom());
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
     * @param mavenProject        maven project instance
     * @param commonDependencies  common dependencies
     * @param testDependencies    test dependencies
     * @param compileDependencies compile dependencies
     * @param runtimeDependencies runtime dependencies
     * @param writer              ebuild writer
     */
    private void writeDependencies(final MavenProject mavenProject,
            final List<ResolvedDependency> commonDependencies,
            final List<ResolvedDependency> testDependencies,
            final List<ResolvedDependency> compileDependencies,
            final List<ResolvedDependency> runtimeDependencies,
            final PrintWriter writer) {
        boolean hasCDepend = false;

        if (!commonDependencies.isEmpty() || !testDependencies.isEmpty()) {
            hasCDepend = true;

            writer.println();
            writer.println("# Common dependencies");

            if (!commonDependencies.isEmpty()) {
                writeDependenciesInfo(writer, commonDependencies, null);
            }

            if (!testDependencies.isEmpty()) {
                writeDependenciesInfo(writer, testDependencies, "test?");
            }

            writer.print("CDEPEND=\"");

            if (!commonDependencies.isEmpty()) {
                sortDependencies(commonDependencies);

                commonDependencies.stream().
                        filter((final ResolvedDependency dependency)
                                -> dependency.getSystemDependency() != null).
                        forEach((final ResolvedDependency dependency) -> {
                            writer.println();
                            writer.print('\t');
                            writer.print(dependency.getSystemDependency());
                        });
            }

            if (!testDependencies.isEmpty()) {
                sortDependencies(testDependencies);

                writer.println();
                writer.println("\ttest? (");

                testDependencies.stream().
                        filter((final ResolvedDependency dependency)
                                -> dependency.getSystemDependency() != null).
                        forEach((final ResolvedDependency dependency) -> {
                            writer.println();
                            writer.print("\t\t");
                            writer.print(dependency.getSystemDependency());
                        });

                writer.print("\t)");
            }

            writer.println('"');
        }

        if (!compileDependencies.isEmpty()) {
            writer.println("# Compile dependencies");
            writeDependenciesInfo(writer, compileDependencies, null);
        } else {
            writer.println();
        }

        writer.print("DEPEND=\"");

        if (hasCDepend) {
            writer.print("${CDEPEND}");
        }

        writer.println();
        writer.print("\t>=virtual/jdk-");
        writer.print(mavenProject.getHigherVersion());

        if (!compileDependencies.isEmpty()) {
            compileDependencies.stream().
                    filter((final ResolvedDependency dependency)
                            -> dependency.getSystemDependency() != null).
                    forEach((final ResolvedDependency dependency) -> {
                        writer.println();
                        writer.print('\t');
                        writer.print(dependency.getSystemDependency());
                    });
        }

        writer.println('"');

        if (!runtimeDependencies.isEmpty()) {
            writer.println("# Runtime dependencies");
            writeDependenciesInfo(writer, runtimeDependencies, null);
        } else {
            writer.println();
        }

        writer.print("RDEPEND=\"");

        if (hasCDepend) {
            writer.print("${CDEPEND}");
        }

        writer.println();
        writer.print("\t>=virtual/jre-");
        writer.print(mavenProject.getHigherVersion());

        if (!runtimeDependencies.isEmpty()) {
            runtimeDependencies.stream().
                    filter((final ResolvedDependency dependency)
                            -> dependency.getSystemDependency() != null).
                    forEach((final ResolvedDependency dependency) -> {
                        writer.println();
                        writer.print('\t');
                        writer.print(dependency.getSystemDependency());
                    });
        }

        writer.println('"');
    }

    /**
     * Writes dependencies information to the ebuild.
     *
     * @param writer       ebuild writer
     * @param dependencies list of dependencies
     * @param useFlag      optional USE flag including question mark
     */
    private void writeDependenciesInfo(final PrintWriter writer,
            final List<ResolvedDependency> dependencies, final String useFlag) {
        dependencies.stream().forEach((ResolvedDependency dependency) -> {
            writer.print("# ");

            if (useFlag != null) {
                writer.print(useFlag);
                writer.print(' ');
            }

            writer.print(dependency.getMavenDependency().getGroupId());
            writer.print(':');
            writer.print(dependency.getMavenDependency().getArtifactId());
            writer.print(':');
            writer.print(dependency.getMavenDependency().getVersion());
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
     * @param config              application configuration
     * @param mavenProject        maven project instance
     * @param commonDependencies  common dependencies
     * @param testDependencies    test dependencies
     * @param compileDependencies compile dependencies
     * @param runtimeDependencies runtime dependencies
     * @param writer              ebuild writer
     */
    private void writeScript(final Config config,
            final MavenProject mavenProject,
            final List<ResolvedDependency> commonDependencies,
            final List<ResolvedDependency> testDependencies,
            final List<ResolvedDependency> compileDependencies,
            final List<ResolvedDependency> runtimeDependencies,
            final PrintWriter writer) {
        writer.println();

        if (!mavenProject.getSourceVersion().equals(mavenProject.
                getTargetVersion())) {
            writer.print("JAVA_SOURCE_VERSION=\"");
            writer.print(mavenProject.getSourceVersion());
            writer.println('"');
            writer.print("JAVA_TARGET_VERSION=\"");
            writer.print(mavenProject.getTargetVersion());
            writer.println('"');
        }

        if (!commonDependencies.isEmpty() || !runtimeDependencies.isEmpty()) {
            final List<ResolvedDependency> dependencies
                    = new ArrayList<>(commonDependencies.size()
                            + runtimeDependencies.size());
            dependencies.addAll(commonDependencies);
            dependencies.addAll(runtimeDependencies);

            writer.print("JAVA_GENTOO_CLASSPATH=\"");
            writer.print(createClassPath(dependencies));
            writer.println('"');
        }

        if (!compileDependencies.isEmpty()) {
            writer.print("JAVA_CLASSPATH_EXTRA=\"");
            writer.print(createClassPath(compileDependencies));
            writer.println('"');
        }

        if (!testDependencies.isEmpty()) {
            writer.print("JAVA_GENTOO_TEST_CLASSPATH=\"");
            writer.print(createClassPath(testDependencies));
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

    /**
     * Container for resolved dependency information.
     */
    private static class ResolvedDependency {

        /**
         * Maven dependency.
         */
        private final MavenProject.Dependency mavenDependency;
        /**
         * System dependency.
         */
        private final String systemDependency;

        /**
         * Creates new instance of ResolvedDependency.
         *
         * @param mavenDependency  {@link #mavenDependency}
         * @param systemDependency {@link #systemDependency}
         */
        ResolvedDependency(final MavenProject.Dependency mavenDependency,
                final String systemDependency) {
            this.mavenDependency = mavenDependency;
            this.systemDependency = systemDependency;
        }

        /**
         * Getter for {@link #mavenDependency}.
         *
         * @return {@link #mavenDependency}
         */
        public MavenProject.Dependency getMavenDependency() {
            return mavenDependency;
        }

        /**
         * Getter for {@link #systemDependency}.
         *
         * @return {@link #systemDependency}
         */
        public String getSystemDependency() {
            return systemDependency;
        }
    }
}
