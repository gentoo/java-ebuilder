package org.gentoo.java.ebuilder.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.gentoo.java.ebuilder.Config;

/**
 * Parser for parsing pom.xml into project collector class.
 *
 * @author fordfrog
 */
public class MavenParser {

    /**
     * Parses specified pom.xml files.
     *
     * @param config     application configuration
     * @param mavenCache maven cache
     *
     * @return list of maven projects
     */
    public List<MavenProject> parsePomFiles(final Config config,
            final MavenCache mavenCache) {
        final List<MavenProject> result
                = new ArrayList<>(config.getPomFiles().size());

        config.getPomFiles().stream().forEach((pomFile) -> {
            final File effectivePom = getEffectivePom(config, pomFile);

            final MavenProject mavenProject = parsePom(config, mavenCache,
                    pomFile, effectivePom);

            if (mavenProject.hasTests()
                    && mavenProject.getTestDependencies().isEmpty()) {
                mavenProject.addDependency(new MavenDependency(
                        "junit", "junit", "4.11", "test",
                        mavenCache.getDependency("junit", "junit", "4.11")));
            }

	    result.add(mavenProject);
        });

        return result;
    }

    /**
     * Consumes current element.
     *
     * @param reader XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while parsing the
     *                            element.
     */
    private void consumeElement(final XMLStreamReader reader)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                consumeElement(reader);
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Stores effective pom to file and returns the file.
     *
     * @param config  application configuration
     * @param pomFile path to pom.xml file that should be processed
     *
     * @return path to effective pom
     */
    private File getEffectivePom(final Config config, final Path pomFile) {
        final File outputPath;

        try {
            outputPath = File.createTempFile("pom", ".xml");
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to create temporary file for "
                    + "effective pom", ex);
        }

        config.getStdoutWriter().print("Retrieving effective pom for "
                + pomFile + " into " + outputPath + "...");

        final ProcessBuilder processBuilder = new ProcessBuilder("mvn", "-f",
                pomFile.toString(), "help:effective-pom",
                "-Doutput=" + outputPath);
        processBuilder.directory(config.getWorkdir().toFile());

        final Process process;

        try {
            process = processBuilder.start();
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to run mvn command", ex);
        }

        try {
            process.waitFor(1, TimeUnit.MINUTES);
        } catch (final InterruptedException ex) {
            config.getErrorWriter().println("ERROR: mvn process did not finish "
                    + "within 1 minute, exiting.");
            Runtime.getRuntime().exit(1);
        }

        if (process.exitValue() != 0) {
            config.getErrorWriter().println(
                    "ERROR: Failed to run mvn command:");

            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line = reader.readLine();

                while (line != null) {
                    config.getErrorWriter().println(line);
                    line = reader.readLine();
                }
            } catch (final IOException ex) {
                throw new RuntimeException(
                        "Failed to read mvn command error output", ex);
            }

            Runtime.getRuntime().exit(1);
        }

        config.getStdoutWriter().println("done");

        return outputPath;
    }

    /**
     * Parses build plugin.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseBuildPlugin(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        String artifactId = null;

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "artifactId":
                        artifactId = reader.getElementText();
                        break;
                    case "configuration":
                        parseBuildPluginConfiguration(mavenProject, reader,
                                artifactId);
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses build plugin configuration.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     * @param artifactId   plugin artifact id
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseBuildPluginConfiguration(final MavenProject mavenProject,
            final XMLStreamReader reader, final String artifactId)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "archive":
                        if ("maven-jar-plugin".equals(artifactId)) {
                            parseBuildPluginConfigurationArchive(mavenProject,
                                    reader);
                        } else {
                            consumeElement(reader);
                        }

                        break;
                    case "source":
                        if ("maven-compiler-plugin".equals(artifactId)) {
                            mavenProject.setSourceVersion(
                                    reader.getElementText());
                        } else {
                            consumeElement(reader);
                        }

                        break;
                    case "target":
                        if ("maven-compiler-plugin".equals(artifactId)) {
                            mavenProject.setTargetVersion(
                                    reader.getElementText());
                        } else {
                            consumeElement(reader);
                        }

                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses archive element of build plugin configuration.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseBuildPluginConfigurationArchive(
            final MavenProject mavenProject, final XMLStreamReader reader)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "manifest":
                        parseManifest(mavenProject, reader);
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses build plugins and its sub-elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseBuildPlugins(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "plugin":
                        parseBuildPlugin(mavenProject, reader);
                        break;
                    default:
                        consumeElement(reader);
                }
            }
        }
    }

    /**
     * Parses manifest elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseManifest(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "mainClass":
                        mavenProject.setMainClass(reader.getElementText());
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses the pom file and returns maven project instance containing
     * collected information.
     *
     * @param config       application configuration
     * @param mavenCache   maven cache
     * @param pomFile      path to pom.xml file
     * @param effectivePom path to effective pom
     *
     * @return maven project instance
     */
    private MavenProject parsePom(final Config config,
            final MavenCache mavenCache, final Path pomFile,
            final File effectivePom) {
        config.getStdoutWriter().print("Parsing effective pom...");

        final XMLStreamReader reader;

        try {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(
                    new FileInputStream(effectivePom));
        } catch (final FactoryConfigurationError | FileNotFoundException |
                XMLStreamException ex) {
            throw new RuntimeException("Failed to read effective pom", ex);
        }

        final MavenProject mavenProject = new MavenProject(pomFile);

        try {
            while (reader.hasNext()) {
                reader.next();

                if (reader.isStartElement()) {
                    switch (reader.getLocalName()) {
                        case "projects":
                            /* no-op */
                            break;
                        case "project":
                            parseProject(mavenProject, mavenCache, reader);
                            break;
                        default:
                            consumeElement(reader);
                    }
                }
            }
        } catch (final XMLStreamException ex) {
            throw new RuntimeException("Failed to parse effective pom", ex);
        }

        config.getStdoutWriter().println("done");

        return mavenProject;
    }

    /**
     * Parses project element and it's sub-elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseProject(final MavenProject mavenProject,
            final MavenCache mavenCache, final XMLStreamReader reader)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "artifactId":
                        mavenProject.setArtifactId(reader.getElementText());
                        break;
                    case "build":
                        parseProjectBuild(mavenProject, reader);
                        break;
                    case "dependencies":
                        parseProjectDependencies(mavenProject, mavenCache,
                                reader);
                        break;
                    case "description":
                        mavenProject.setDescription(reader.getElementText());
                        break;
                    case "groupId":
                        mavenProject.setGroupId(reader.getElementText());
                        break;
                    case "properties":
                        parseProjectProperties(mavenProject, reader);
                        break;
                    case "url":
                        mavenProject.setHomepage(reader.getElementText());
                        break;
                    case "version":
                        mavenProject.setVersion(reader.getElementText().replace(
                                "-SNAPSHOT", ""));
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses project build element and its sub-elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseProjectBuild(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "plugins":
                        parseBuildPlugins(mavenProject, reader);
                        break;
                    case "resources":
                        parseResources(mavenProject, reader);
                        break;
                    case "sourceDirectory":
                        mavenProject.setSourceDirectory(
                                Paths.get(reader.getElementText()));
                        break;
                    case "testResources":
                        parseTestResources(mavenProject, reader);
                        break;
                    case "testSourceDirectory":
                        mavenProject.setTestSourceDirectory(
                                Paths.get(reader.getElementText()));
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses project dependencies and its sub-elements.
     *
     * @param mavenProject maven project instance
     * @param mavenCache   maven cache
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseProjectDependencies(final MavenProject mavenProject,
            final MavenCache mavenCache, final XMLStreamReader reader)
            throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "dependency":
                        parseProjectDependency(mavenProject, mavenCache, reader);
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses project dependency.
     *
     * @param mavenProject maven project instance
     * @param mavenCache   maven cache
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseProjectDependency(final MavenProject mavenProject,
            final MavenCache mavenCache, final XMLStreamReader reader)
            throws XMLStreamException {
        String groupId = null;
        String artifactId = null;
        String version = null;
        String scope = null;

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "artifactId":
                        artifactId = reader.getElementText();
                        break;
                    case "groupId":
                        groupId = reader.getElementText();
                        break;
                    case "scope":
                        scope = reader.getElementText();
                        break;
                    case "version":
                        version = reader.getElementText().replace("-SNAPSHOT",
                                "");
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                mavenProject.addDependency(new MavenDependency(groupId,
                        artifactId, version, scope, mavenCache.getDependency(
                                groupId, artifactId, version)));

                return;
            }
        }
    }

    /**
     * Parses project properties.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading the
     *                            XML stream.
     */
    private void parseProjectProperties(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "maven.compiler.source":
                        mavenProject.setSourceVersion(reader.getElementText());
                        break;
                    case "maven.compiler.target":
                        mavenProject.setTargetVersion(reader.getElementText());
                        break;
                    case "project.build.sourceEncoding":
                        mavenProject.setSourceEncoding(reader.getElementText());
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses resource element.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseResource(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "directory":
                        mavenProject.addResourceDirectory(
                                Paths.get(reader.getElementText()));
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses resources and its sub-elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseResources(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "resource":
                        parseResource(mavenProject, reader);
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses test resource.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseTestResource(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "directory":
                        mavenProject.addTestResourceDirectory(
                                Paths.get(reader.getElementText()));
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Parses test resources and its sub-elements.
     *
     * @param mavenProject maven project instance
     * @param reader       XML stream reader
     *
     * @throws XMLStreamException Thrown if problem occurred while reading XML
     *                            stream.
     */
    private void parseTestResources(final MavenProject mavenProject,
            final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "testResource":
                        parseTestResource(mavenProject, reader);
                        break;
                    default:
                        consumeElement(reader);
                }
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }
}
