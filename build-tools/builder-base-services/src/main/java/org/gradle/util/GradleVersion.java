package org.gradle.util;

import org.gradle.util.internal.DefaultGradleVersion;

/**
 * Represents a Gradle version.
 */
public abstract class GradleVersion implements Comparable<GradleVersion> {

    /**
     * This field only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public static final String URL = "https://www.gradle.org";

    /**
     * This field only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public static final String RESOURCE_NAME = "/org/gradle/build-receipt.properties";

    /**
     * This field only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public static final String VERSION_OVERRIDE_VAR = "GRADLE_VERSION_OVERRIDE";

    /**
     * This field only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public static final String VERSION_NUMBER_PROPERTY = "versionNumber";

    /**
     * Returns the current Gradle version.
     *
     * @return The current Gradle version.
     */
    public static GradleVersion current() {
        return DefaultGradleVersion.current();
    }

    /**
     * Parses the given string into a GradleVersion.
     *
     * @throws IllegalArgumentException On unrecognized version string.
     */
    public static GradleVersion version(String version) throws IllegalArgumentException {
        return DefaultGradleVersion.version(version);
    }

    /**
     * Returns the string that represents this version.
     *
     * @return this Gradle version in string format.
     */
    public abstract String getVersion();

    /**
     * This method only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public abstract String getBuildTime();

    /**
     * This method only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public abstract String getRevision();

    /**
     * Returns {@code true} if this instance represent a snapshot version (e.g. 7.0-20210406233629+0000).
     *
     * @return Whether the current instance is a snapshot version
     */
    public abstract boolean isSnapshot();

    /**
     * The base version of this version. For pre-release versions, this is the target version.
     *
     * For example, the version base of '7.0-rc-1' is '7.0'.
     *
     * @return The version base
     */
    public abstract GradleVersion getBaseVersion();

    /**
     * This method only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public abstract GradleVersion getNextMajor();

    /**
     * This method only kept here to maintain binary compatibility.
     *
     * @deprecated will be removed in Gradle 8.
     */
    @Deprecated
    public abstract boolean isValid();

    @Override
    public abstract int compareTo(GradleVersion o);
}
