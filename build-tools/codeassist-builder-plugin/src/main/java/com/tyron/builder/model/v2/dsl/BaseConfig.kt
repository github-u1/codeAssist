package com.tyron.builder.model.v2.dsl

import com.tyron.builder.model.v2.AndroidModel
import java.io.File

/**
 * Base config object for Build Type and Product flavor.
 *
 * This is an interface for the gradle tooling api, and should only be used from Android Studio.
 * It is not part of the DSL & API interfaces of the Android Gradle Plugin.
 *
 * @since 4.2
 */
interface BaseConfig: AndroidModel {

    /**
     * The name
     */
    val name: String

    /**
     * The application id suffix applied to this base config.
     */
    val applicationIdSuffix: String?

    /**
     * The version name suffix of this flavor or null if none have been set.
     * This is only the value set on this product flavor, not necessarily the actual
     * version name suffix used.
     */
    val versionNameSuffix: String?

    /**
     * Map of Build Config Fields where the key is the field name.
     * If null, buildConfig is disabled in the project.
     */
    val buildConfigFields: Map<String, ClassField>?

    /**
     * Map of generated res values where the key is the res name.
     * If null, res values are disabled in the project.
     */
    val resValues: Map<String, ClassField>?

    /**
     * Specifies the ProGuard configuration files that the plugin should use.
     *
     * There are two ProGuard rules files that ship with the Android plugin and are used by
     * default:
     *
     *  * proguard-android.txt
     *  * proguard-android-optimize.txt
     *
     * `proguard-android-optimize.txt` is identical to `proguard-android.txt`,
     * except with optimizations enabled. You can use [getDefaultProguardFile(String)]
     * to return the full path of the files.
     *
     * @return a non-null collection of files.
     * @see .getTestProguardFiles
     */
    val proguardFiles: Collection<File>

    /** The collection of proguard rule files for consumers of the library to use. */
    val consumerProguardFiles: Collection<File>

    /** The collection of proguard rule files to use for the test APK. */
    val testProguardFiles: Collection<File>

    /**
     * The map of key value pairs for placeholder substitution in the android manifest file.
     *
     * This map will be used by the manifest merger.
     */
    val manifestPlaceholders: Map<String, Any>

    /**
     * Returns whether multi-dex is enabled.
     *
     * This can be null if the flag is not set, in which case the default value is used.
     */
    val multiDexEnabled: Boolean?

    val multiDexKeepFile: File?
    val multiDexKeepProguard: File?

    /**
     * Whether this is marked as a default value in its dimension
     */
    val isDefault: Boolean?
}
