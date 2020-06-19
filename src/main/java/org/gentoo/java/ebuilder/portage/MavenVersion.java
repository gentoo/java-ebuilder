package org.gentoo.java.ebuilder.portage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maven version container.
 *
 * @author fordfrog
 */
public class MavenVersion implements Comparable<MavenVersion> {

    /**
     * Pattern for parsing maven version number.
     */
    private static final Pattern PATTERN_VERSION = Pattern.compile(
            "^[vr]?(\\d+)(?:\\.(\\d+))?(?:(?:\\.|b|beta)(\\d+))?(?:[\\.-]?(.*))?$");
    /**
     * Pattern for parsing maven version range.
     */
    private static final Pattern PATTERN_VERSION_RANGE
            = Pattern.compile("[\\[\\(](.*), ?(.*?)[\\]\\)]");
    /**
     * Incremental version number.
     */
    private final int incrementalVersion;
    /**
     * Major version number.
     */
    private final int majorVersion;
    /**
     * Minor version number.
     */
    private final int minorVersion;
    /**
     * Version qualifier.
     */
    private final String qualifier;
    /**
     * Original version string.
     */
    private final String version;

    /**
     * Creates new instance of MavenVersion.
     *
     * @param version version string
     */
    public MavenVersion(final String version) {
        this.version = version;

        final Matcher matcherRange = PATTERN_VERSION_RANGE.matcher(version);
        final String useVersion;

        if (matcherRange.matches()) {
            useVersion = matcherRange.group(1);
        } else {
            useVersion = version;
        }

        final Matcher matcher = PATTERN_VERSION.matcher(useVersion);

        if (!matcher.matches()) {
            throw new RuntimeException(
                    "Maven version " + useVersion + " is not valid.");
        }

        majorVersion = Integer.parseInt(matcher.group(1), 10);
        minorVersion = matcher.group(2) == null
                ? 0 : Integer.parseInt(matcher.group(2), 10);
        incrementalVersion = matcher.group(3) == null
                ? 0 : Integer.parseInt(matcher.group(3), 10);
        qualifier = matcher.group(4) == null ? "" : matcher.group(4).toLowerCase();
    }

    @Override
    public int compareTo(final MavenVersion o) {
        if (o == null) {
            return 1;
        } else if (majorVersion != o.getMajorVersion()) {
            return Integer.valueOf(majorVersion).compareTo(o.getMajorVersion());
        } else if (minorVersion != o.getMinorVersion()) {
            return Integer.valueOf(minorVersion).compareTo(o.getMinorVersion());
        } else if (incrementalVersion != o.getIncrementalVersion()) {
            return Integer.valueOf(incrementalVersion).compareTo(o.getIncrementalVersion());
        } else {
            return qualifier.compareTo(o.getQualifier());
        }
    }

    /**
     * Getter for {@link #incrementalVersion}.
     *
     * @return {@link #incrementalVersion}
     */
    public int getIncrementalVersion() {
        return incrementalVersion;
    }

    /**
     * Getter for {@link #majorVersion}.
     *
     * @return {@link #majorVersion}
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Getter for {@link #minorVersion}.
     *
     * @return {@link #minorVersion}
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Getter for {@link #qualifier}.
     *
     * @return {@link #qualifier}
     */
    public String getQualifier() {
        return qualifier;
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
