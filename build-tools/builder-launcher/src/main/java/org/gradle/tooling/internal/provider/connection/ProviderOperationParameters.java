package org.gradle.tooling.internal.provider.connection;

import org.gradle.api.logging.LogLevel;
import org.gradle.tooling.internal.protocol.InternalBuildProgressListener;
import org.gradle.tooling.internal.protocol.InternalLaunchable;
import org.gradle.tooling.internal.protocol.ProgressListenerVersion1;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Defines what information is needed on the provider side regarding the build operation.
 *
 * This is used as an adapter over the {@link org.gradle.tooling.internal.consumer.parameters.ConsumerOperationParameters} instance provided by the consumer.
 */
public interface ProviderOperationParameters {
    boolean getVerboseLogging();

    LogLevel getBuildLogLevel();

    /**
     * @return When null, assume empty stdin (rather than consume from the current process' stdin).
     */
    @Nullable
    InputStream getStandardInput();

    /**
     * @return When null, use the provider's default Java home.
     */
    @Nullable
    File getJavaHome();

    /**
     * @return When null, use the provider's default JVM arguments. When empty, use no JVM arguments.
     */
    @Nullable
    List<String> getJvmArguments();

    /**
     * @return When null, use the provider's default environment variables. When empty, use no environment variables.
     * @since 3.5-rc-1
     */
    @Nullable
    Map<String, String> getEnvironmentVariables(@Nullable Map<String, String> defaultValue);

    /**
     * @since 1.0-milestone-3
     */
    long getStartTime();

    /**
     * @return When null, use the provider's default Gradle user home dir.
     * @since 1.0-milestone-3
     */
    @Nullable
    File getGradleUserHomeDir();

    /**
     * @since 1.0-milestone-3
     */
    File getProjectDir();

    /**
     * @return When null, use the provider's default value for search upwards.
     * @since 1.0-milestone-3
     * @deprecated The ability to change the search upward behavior when calling a build has been removed in Gradle 7.0. This is kept for compatibility with pre 7.0 versions.
     */
    @Deprecated
    @Nullable
    Boolean isSearchUpwards();

    /**
     * @return When null, use the provider's default value for embedded.
     * @since 1.0-milestone-3
     */
    @Nullable
    Boolean isEmbedded();

    /**
     * @return When null, use the provider's default value for color output.
     * @since 2.3-rc-1
     */
    Boolean isColorOutput();

    /**
     * @return When null, discard the stdout (rather than forward to the current process' stdout)
     * @since 1.0-milestone-3
     */
    @Nullable
    OutputStream getStandardOutput();

    /**
     * @return When null, discard the stderr (rather than forward to the current process' stdout)
     * @since 1.0-milestone-3
     */
    @Nullable
    OutputStream getStandardError();

    /**
     * @return When null, use the provider's default daemon idle timeout
     * @since 1.0-milestone-3
     */
    @Nullable
    Integer getDaemonMaxIdleTimeValue();

    /**
     * @return Must not return null when {@link #getDaemonMaxIdleTimeValue()} returns a non-null value. Otherwise, unspecified.
     * @since 1.0-milestone-3
     */
    @Nullable
    TimeUnit getDaemonMaxIdleTimeUnits();

    /**
     * @return When null, use the provider's default daemon base dir.
     * @since 2.2-rc-1
     */
    @Nullable
    File getDaemonBaseDir();

    /**
     * @since 1.0-milestone-3
     */
    ProgressListenerVersion1 getProgressListener();

    /**
     * @return When null, do not forward any build progress events.
     * @since 2.4-rc-1
     */
    @Nullable
    InternalBuildProgressListener getBuildProgressListener();

    /**
     * @return When null, assume no arguments.
     */
    @Nullable
    List<String> getArguments();

    /**
     * @return When null, no tasks should be run. When empty, use the default tasks
     */
    @Nullable
    List<String> getTasks();

    /**
     * @since 1.12-rc-1
     */
    List<InternalLaunchable> getLaunchables();

    /**
     * @return When empty, do not inject a plugin classpath.
     * @since 2.8-rc-1
     */
    List<File> getInjectedPluginClasspath();
}
