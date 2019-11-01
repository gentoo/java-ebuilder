package org.gentoo.java.ebuilder.portage;

/**
 * Container for cache item information.
 *
 * @author fordfrog
 */
public class CacheItem {

    /**
     * Maven artifact id.
     */
    private final String artifactId;
    /**
     * Portage category.
     */
    private final String category;
    /**
     * Maven group id.
     */
    private final String groupId;
    /**
     * Main Java eclass inherited.
     */
    private final String javaEclass;
    /**
     * Maven version (of package jar).
     */
    private final String mavenVersion;
    /**
     * Parsed maven version.
     */
    private final MavenVersion parsedMavenVersion;
    /**
     * Portage package.
     */
    private final String pkg;
    /**
     * Portage ebuild slot.
     */
    private final String slot;
    /**
     * Portage USE flag that enables java in the package in case of optional
     * java in the package.
     */
    private final String useFlag;
    /**
     * Portage ebuild version.
     */
    private final String version;

    /**
     * Creates new instance of CacheItem.
     *
     * @param category     {@link #category}
     * @param pkg          {@link #pkg}
     * @param version      {@link #version}
     * @param slot         {@link #slot}
     * @param useFlag      {@link #useFlag}
     * @param groupId      {@link #groupId}
     * @param artifactId   {@link #artifactId}
     * @param mavenVersion {@link #mavenVersion}
     * @param javaEclass   {@link #javaEclass}
     */
    public CacheItem(final String category, final String pkg,
            final String version, final String slot, final String useFlag,
            final String groupId, final String artifactId,
            final String mavenVersion, final String javaEclass) {
        this.category = category;
        this.pkg = pkg;
        this.version = version;
        this.slot = slot;
        this.useFlag = useFlag;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.mavenVersion = mavenVersion;
        this.javaEclass = javaEclass;

        parsedMavenVersion = mavenVersion == null
                ? null : new MavenVersion(mavenVersion);
    }

    /**
     * Creates new instance of CacheItem.
     *
     * @param line line from cache file
     */
    public CacheItem(final String line) {
        final String[] parts = line.split(":");

        try {
            category = parts[0];
            pkg = parts[1];
            version = parts[2];
            slot = parts[3];

            if (parts.length > 4) {
                useFlag = parts[4].isEmpty() ? null : parts[4];
            } else {
                useFlag = null;
            }

            if (parts.length > 5) {
                groupId = parts[5].isEmpty() ? null : parts[5];
                artifactId = parts[6].isEmpty() ? null : parts[6];
                mavenVersion = parts[7].isEmpty() ? null : parts[7];
            } else {
                groupId = null;
                artifactId = null;
                mavenVersion = null;
            }

            if (parts.length > 8) {
                javaEclass = parts[8].isEmpty() ? null : parts[8];
            } else {
                javaEclass = null;
            }
        } catch (final ArrayIndexOutOfBoundsException ex) {
            throw new RuntimeException("Failed to parse cache line: " + line,
                    ex);
        }

        parsedMavenVersion = mavenVersion == null
                ? null : new MavenVersion(mavenVersion);
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
     * Getter for {@link #category}.
     *
     * @return {@link #category}
     */
    public String getCategory() {
        return category;
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
     * Getter for {@link #javaEclass}.
     *
     * @return {@link #javaEclass}
     */
    public String getJavaEclass() {
        return javaEclass;
    }

    /**
     * Getter for {@link #mavenVersion}.
     *
     * @return {@link #mavenVersion}
     */
    public String getMavenVersion() {
        return mavenVersion;
    }

    /**
     * Getter for {@link #parsedMavenVersion}.
     *
     * @return {@link #parsedMavenVersion}
     */
    public MavenVersion getParsedMavenVersion() {
        return parsedMavenVersion;
    }

    /**
     * Getter for {@link #pkg}.
     *
     * @return {@link #pkg}
     */
    public String getPkg() {
        return pkg;
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
     * Getter for {@link #useFlag}.
     *
     * @return {@link #useFlag}
     */
    public String getUseFlag() {
        return useFlag;
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
