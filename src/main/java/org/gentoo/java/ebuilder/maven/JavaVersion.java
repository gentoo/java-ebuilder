package org.gentoo.java.ebuilder.maven;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java versionNumber container.
 *
 * @author fordfrog
 */
public class JavaVersion {

    /**
     * Pattern for parsing Java versionNumber.
     */
    private static final Pattern PATTERN_VERSION
            = Pattern.compile("^(?:1\\.)?(\\d+)$");
    /**
     * Java versionNumber number.
     */
    private final int versionNumber;
    /**
     * Java version string.
     */
    private final String versionString;

    /**
     * Creates new instance of JavaVersion. Parses the version string and uses
     * the main Java version. Terminates if the version is not valid.
     *
     * @param versionString version string
     */
    public JavaVersion(final String versionString) {
        final Matcher matcher = PATTERN_VERSION.matcher(versionString);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Java version \"{0}\" is not valid!", versionString));
        }

        versionNumber = Integer.parseInt(matcher.group(1), 10);
        if (versionNumber <= 8) {
            this.versionString = "1.8";
        }
        else {
            this.versionString = versionString;
        }

    }

    /**
     * Compares this JavaVersion to the passed JavaVersion.
     *
     * @param javaVersion JavaVersion to compare to.
     *
     * @return -1 if this versionNumber is lower, 1 if this versionNumber is
     * higher and 0 if both are equal
     */
    public int compareTo(final JavaVersion javaVersion) {
        if (javaVersion.getVersionNumber() > versionNumber) {
            return -1;
        } else if (javaVersion.getVersionNumber() < versionNumber) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Getter for {@link #versionNumber}.
     *
     * @return {@link #versionNumber}
     */
    public int getVersionNumber() {
        return versionNumber;
    }

    /**
     * Getter for {@link #versionString}.
     *
     * @return {@link #versionString}
     */
    public String getVersionString() {
        return versionString;
    }

    @Override
    public String toString() {
        return versionString;
    }
}
