package org.gentoo.java.ebuilder;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.gentoo.java.ebuilder.maven.JavaVersion;
import org.gentoo.java.ebuilder.portage.KeywordComparator;

/**
 * Container for command line configuration.
 *
 * @author fordfrog
 */
public class Config {

    /**
     * Path to portage ebuild cache.
     */
    private Path cacheFile = Paths.get(System.getProperty("user.home"),
            ".java-ebuilder/cache");
    /**
     * URI that goes to pre-compiled Maven Jar.
     */
    private URI binjarUri;
    /**
     * whether binjarUri is set.
     */
    private boolean binjarUriExists;
    /**
     * URI that goes to SRC_URI.
     */
    private URI downloadUri;
    /**
     * Whether to output information about parsed projects.
     */
    private boolean dumpProjects;
    /**
     * Path to ebuild file that should be generated.
     */
    private Path ebuild;
    /**
     * Ebuild name.
     */
    private String ebuildName;
    /**
     * Ebuild version excluding suffix.
     */
    private String ebuildVersion;
    /**
     * Ebuild version suffix (-r).
     */
    private String ebuildVersionSuffix;
    /**
     * Writer for errors.
     */
    private final PrintWriter errorWriter;
    /**
     * JDK/JRE version that will be used if version in POM files is lower than
     * this one.
     */
    private JavaVersion forceMinJavaVersion;
    /**
     * Whethe the source code is distributed by Maven Central
     */
    private boolean fromMavenCentral;
    /**
     * Whether ebuild should be generated.
     */
    private boolean generateEbuild;
    /**
     * Arch keywords.
     */
    private SortedSet<String> keywords = new TreeSet<>(new KeywordComparator());
    /**
     * License name.
     */
    private String license;
    /**
     * List of paths to pom.xml files.
     */
    private final List<Path> pomFiles = new ArrayList<>(10);
    /**
     * Path to portage tree.
     */
    private SortedSet<Path> portageTree = new TreeSet<>();
    /**
     * Whether ebuild cache should be refreshed.
     */
    private boolean refreshCache;
    /**
     * SLOT number.
     */
    private String slot;
    /**
     * Writer for standard output.
     */
    private final PrintWriter stdoutWriter;
    /**
     * URI that goes to *-test-sources.jar distributed by maven central
     */
    private URI testSrcUri;
    /**
     * whether testSrcUri is set and whether the pkg is from maven central.
     */
    private boolean testSrcUriExists;
    /**
     * Path to workdir.
     */
    private Path workdir;

    /**
     * Creates new instance of Config.
     *
     * @param stdoutWriter {@link #stdoutWriter}
     * @param errorWriter  {@link #errorWriter}
     */
    public Config(final PrintWriter stdoutWriter,
            final PrintWriter errorWriter) {
        this.stdoutWriter = stdoutWriter;
        this.errorWriter = errorWriter;
    }

    /**
     * Adds pom file to {@link #pomFiles}.
     *
     * @param pomFile path to pom file
     */
    public void addPomFile(final Path pomFile) {
        pomFiles.add(pomFile);
    }

    /**
     * Getter for {@link #cacheFile}.
     *
     * @return {@link #cacheFile}
     */
    public Path getCacheFile() {
        return cacheFile;
    }

    /**
     * Setter for {@link #cacheFile}.
     *
     * @param cacheFile {@link #cacheFile}
     */
    public void setCacheFile(final Path cacheFile) {
        this.cacheFile = cacheFile;
    }

    /**
     * Getter for {@link #binjarUri}.
     *
     * @return {@link #binjarUri}
     */
    public URI getBinjarUri() {
        return binjarUri;
    }

    /**
     * Getter for {@link #binjarUriExists}.
     *
     * @return {@link #binjarUriExists}
     */
    public boolean hasBinjarUri() {
        return binjarUriExists;
    }

    /**
     * Setter for {@link #binjarUri}.
     *
     * @param binjarUri {@link #binjarUri}
     */
    public void setBinjarUri(final URI binjarUri) {
        this.binjarUri = binjarUri;
        this.binjarUriExists = true;
    }

    /**
     * Getter for {@link #downloadUri}.
     *
     * @return {@link #downloadUri}
     */
    public URI getDownloadUri() {
        return downloadUri;
    }

    /**
     * Setter for {@link #downloadUri}.
     *
     * @param downloadUri {@link #downloadUri}
     */
    public void setDownloadUri(final URI downloadUri) {
        this.downloadUri = downloadUri;
    }

    /**
     * Getter for {@link #ebuild}.
     *
     * @return {@link #ebuild}
     */
    public Path getEbuild() {
        return ebuild;
    }

    /**
     * Setter for {@link #ebuild}.
     *
     * @param ebuild {@link #ebuild}
     */
    public void setEbuild(final Path ebuild) {
        this.ebuild = ebuild;
    }

    /**
     * Getter for {@link #ebuildName}.
     *
     * @return {@link #ebuildName}
     */
    public String getEbuildName() {
        return ebuildName;
    }

    /**
     * Setter for {@link #ebuildName}.
     *
     * @param ebuildName {@link #ebuildName}
     */
    public void setEbuildName(final String ebuildName) {
        this.ebuildName = ebuildName;
    }

    /**
     * Getter for {@link #ebuildVersion}.
     *
     * @return {@link #ebuildVersion}
     */
    public String getEbuildVersion() {
        return ebuildVersion;
    }

    /**
     * Setter for {@link #ebuildVersion}.
     *
     * @param ebuildVersion {@link #ebuildVersion}
     */
    public void setEbuildVersion(final String ebuildVersion) {
        this.ebuildVersion = ebuildVersion;
    }

    /**
     * Getter for {@link #ebuildVersionSuffix}.
     *
     * @return {@link #ebuildVersionSuffix}
     */
    public String getEbuildVersionSuffix() {
        return ebuildVersionSuffix;
    }

    /**
     * Setter for {@link #ebuildVersionSuffix}.
     *
     * @param ebuildVersionSuffix {@link #ebuildVersionSuffix}
     */
    public void setEbuildVersionSuffix(final String ebuildVersionSuffix) {
        this.ebuildVersionSuffix = ebuildVersionSuffix;
    }

    /**
     * Getter for {@link #errorWriter}.
     *
     * @return {@link #errorWriter}
     */
    public PrintWriter getErrorWriter() {
        return errorWriter;
    }

    /**
     * Getter for {@link #forceMinJavaVersion}.
     *
     * @return {@link #forceMinJavaVersion}
     */
    public JavaVersion getForceMinJavaVersion() {
        return forceMinJavaVersion;
    }

    /**
     * Setter for {@link #forceMinJavaVersion}.
     *
     * @param forceMinJavaVersion {@link #forceMinJavaVersion}
     */
    public void setForceMinJavaVersion(final JavaVersion forceMinJavaVersion) {
        this.forceMinJavaVersion = forceMinJavaVersion;
    }

    /**
     * Getter for {@link #keywords}.
     *
     * @return {@link #keywords}
     */
    public String getKeywords() {
        return String.join(" ", keywords);
    }

    /**
     * add keyword to {@link #keywords}.
     *
     * @param keyword String that contains one or more keywords
     */
    public void addKeywords(final String keywords) {
        String[] parts = keywords.split(" ");

        /**
         * Make "-amd64" replace "amd64 ~amd64"
         * Make "amd64" replace "~amd64"
         */
        for (String part : parts) {
            if (part.startsWith("-")) {
                this.keywords.remove(part.substring(1));
                this.keywords.remove("~" + part.substring(1));
                this.keywords.add(part);
            } else if (part.startsWith("~")) {
                this.keywords.add(part);
            } else {
                this.keywords.remove("~" + part);
                this.keywords.add(part);
            }
        }
    }

    /**
     * Getter for {@link #license}.
     *
     * @return {@link #license}
     */
    public String getLicense() {
        return license;
    }

    /**
     * Setter for {@link #license}.
     *
     * @param license {@link #license}
     */
    public void setLicense(final String license) {
        this.license = license;
    }

    /**
     * Getter for {@link #pomFiles}.
     *
     * @return {@link #pomFiles}
     */
    public List<Path> getPomFiles() {
        return Collections.unmodifiableList(pomFiles);
    }

    /**
     * Getter for {@link #portageTree}.
     *
     * @return {@link #portageTree}
     */
    public SortedSet<Path> getPortageTree() {
        return portageTree;
    }

    /**
     * Add portageTrees to {@link #portageTree}.
     *
     * @param portageTree {@link #portageTree}
     */
    public void addPortageTree(final Path portageTree) {
        this.portageTree.add(portageTree);
    }

    /**
     * Getter for {@link #slot}.
     *
     * @return {@link #slot}
     */
    public String getSlot() {
        return slot;
    }

    /**
     * Setter for {@link #slot}.
     *
     * @param slot {@link #slot}
     */
    public void setSlot(String slot) {
        this.slot = slot;
    }

    /**
     * Getter for {@link #stdoutWriter}.
     *
     * @return {@link #stdoutWriter}
     */
    public PrintWriter getStdoutWriter() {
        return stdoutWriter;
    }

    /**
     * Getter for {@link #testSrcUri}.
     *
     * @return {@link #testSrcUri}
     */
    public URI getTestSrcUri() {
        return testSrcUri;
    }

    /**
     * Getter for {@link #testSrcUriExists}.
     *
     * @return {@link #testSrcUriExists}
     */
    public boolean hasTestSrcUri() {
        return testSrcUriExists;
    }

    /**
     * Setter for {@link #testSrcUri}.
     *
     * @param testSrcUri {@link #testSrcUri}
     */
    public void setTestSrcUri(final URI testSrcUri) {
        this.testSrcUri = testSrcUri;
        if (isFromMavenCentral()) {
            this.testSrcUriExists = true;
        }
    }
    /**
     * Getter for {@link #workdir}.
     *
     * @return {@link #workdir}
     */
    public Path getWorkdir() {
        return workdir;
    }

    /**
     * Setter for {@link #workdir}.
     *
     * @param workdir {@link #workdir}
     */
    public void setWorkdir(final Path workdir) {
        this.workdir = workdir;
    }

    /**
     * Getter for {@link #dumpProjects}.
     *
     * @return {@link #dumpProjects}
     */
    public boolean isDumpProjects() {
        return dumpProjects;
    }

    /**
     * Setter for {@link #dumpProjects}.
     *
     * @param dumpProjects {@link #dumpProjects}
     */
    public void setDumpProjects(final boolean dumpProjects) {
        this.dumpProjects = dumpProjects;
    }

    /**
     * Getter for {@link #fromMavenCentral}.
     *
     * @return {@link #fromMavenCentral}
     */
    public boolean isFromMavenCentral() {
        return fromMavenCentral;
    }

    /**
     * Setter for {@link #fromMavenCentral}.
     *
     * @param fromMavenCentral {@link #fromMavenCentral}
     */
    public void setFromMavenCentral(final boolean fromMavenCentral) {
        this.fromMavenCentral = fromMavenCentral;
        if (getTestSrcUri() != null) {
            this.testSrcUriExists = true;
        }
    }

    /**
     * Getter for {@link #generateEbuild}.
     *
     * @return {@link #generateEbuild}
     */
    public boolean isGenerateEbuild() {
        return generateEbuild;
    }

    /**
     * Setter for {@link #generateEbuild}.
     *
     * @param generateEbuild {@link #generateEbuild}
     */
    public void setGenerateEbuild(final boolean generateEbuild) {
        this.generateEbuild = generateEbuild;
    }

    /**
     * Getter for {@link #refreshCache}.
     *
     * @return {@link #refreshCache}
     */
    public boolean isRefreshCache() {
        return refreshCache;
    }

    /**
     * Setter for {@link #refreshCache}.
     *
     * @param refreshCache {@link #refreshCache}
     */
    public void setRefreshCache(final boolean refreshCache) {
        this.refreshCache = refreshCache;
    }
}
