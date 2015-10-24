package org.gentoo.java.ebuilder;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Container for command line configuration.
 *
 * @author fordfrog
 */
public class Config {

    /**
     * Path to portage ebuild cache.
     */
    private final Path cacheFile = Paths.get(System.getProperty("user.home"),
            ".java-ebuilder/cache");
    /**
     * URI that goes to SRC_URI.
     */
    private URI downloadUri;
    /**
     * Path to ebuild file that should be generated.
     */
    private Path ebuild;
    /**
     * Writer for errors.
     */
    private final PrintWriter errorWriter;
    /**
     * Whether ebuild should be generated.
     */
    private boolean generateEbuild;
    /**
     * Path to pom.xml file.
     */
    private Path pom;
    /**
     * Path to portage tree.
     */
    private Path portageTree;
    /**
     * Whether ebuild cache should be refreshed.
     */
    private boolean refreshCache;
    /**
     * Writer for standard output.
     */
    private final PrintWriter stdoutWriter;
    /**
     * Path to tarball root.
     */
    private Path tarballRoot = Paths.get(".");

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
     * Getter for {@link #cacheFile}.
     *
     * @return {@link #cacheFile}
     */
    public Path getCacheFile() {
        return cacheFile;
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
     * Getter for {@link #errorWriter}.
     *
     * @return {@link #errorWriter}
     */
    public PrintWriter getErrorWriter() {
        return errorWriter;
    }

    /**
     * Getter for {@link #pom}.
     *
     * @return {@link #pom}
     */
    public Path getPom() {
        return pom;
    }

    /**
     * Setter for {@link #pom}.
     *
     * @param pom {@link #pom}
     */
    public void setPom(final Path pom) {
        this.pom = pom;
    }

    /**
     * Getter for {@link #portageTree}.
     *
     * @return {@link #portageTree}
     */
    public Path getPortageTree() {
        return portageTree;
    }

    /**
     * Setter for {@link #portageTree}.
     *
     * @param portageTree {@link #portageTree}
     */
    public void setPortageTree(final Path portageTree) {
        this.portageTree = portageTree;
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
     * Getter for {@link #tarballRoot}.
     *
     * @return {@link #tarballRoot}
     */
    public Path getTarballRoot() {
        return tarballRoot;
    }

    /**
     * Setter for {@link #tarballRoot}.
     *
     * @param tarballRoot {@link #tarballRoot}
     */
    public void setTarballRoot(final Path tarballRoot) {
        this.tarballRoot = tarballRoot;
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
