package org.gradle.process.internal;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.jvm.ModularitySpec;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.internal.jvm.DefaultModularitySpec;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.process.JavaExecSpec;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.gradle.process.internal.DefaultExecSpec.copyBaseExecSpecTo;


public class DefaultJavaExecSpec extends DefaultJavaForkOptions implements JavaExecSpec, ProcessArgumentsSpec.HasExecutable {

    private boolean ignoreExitValue;
    private final ProcessStreamsSpec streamsSpec = new ProcessStreamsSpec();
    private final ProcessArgumentsSpec argumentsSpec = new ProcessArgumentsSpec(this);

    private final Property<String> mainClass;
    private final Property<String> mainModule;
    private final ModularitySpec modularity;

    private final FileCollectionFactory fileCollectionFactory;
    private ConfigurableFileCollection classpath;

    @Inject
    public DefaultJavaExecSpec(
        ObjectFactory objectFactory,
        PathToFileResolver resolver,
        FileCollectionFactory fileCollectionFactory
    ) {
        super(resolver, fileCollectionFactory, objectFactory.newInstance(DefaultJavaDebugOptions.class));
        this.mainClass = objectFactory.property(String.class);
        this.mainModule = objectFactory.property(String.class);
        this.modularity = objectFactory.newInstance(DefaultModularitySpec.class);
        this.fileCollectionFactory = fileCollectionFactory;
        this.classpath = fileCollectionFactory.configurableFiles("classpath");
    }

    public void copyTo(JavaExecSpec targetSpec) {
        // JavaExecSpec
        targetSpec.setArgs(getArgs());
        targetSpec.getArgumentProviders().addAll(getArgumentProviders());
        targetSpec.getMainClass().set(getMainClass());
        targetSpec.getMainModule().set(getMainModule());
        targetSpec.getModularity().getInferModulePath().set(getModularity().getInferModulePath());
        targetSpec.classpath(getClasspath());
        // BaseExecSpec
        copyBaseExecSpecTo(this, targetSpec);
        // Java fork options
        super.copyTo(targetSpec);
    }

    @Override
    public List<String> getCommandLine() {
        return argumentsSpec.getCommandLine();
    }

    @Override
    public JavaExecSpec args(Object... args) {
        argumentsSpec.args(args);
        return this;
    }

    @Override
    public JavaExecSpec args(Iterable<?> args) {
        argumentsSpec.args(args);
        return this;
    }

    @Override
    public JavaExecSpec setArgs(@Nullable List<String> arguments) {
        argumentsSpec.setArgs(arguments);
        return this;
    }

    @Override
    public JavaExecSpec setArgs(@Nullable Iterable<?> arguments) {
        argumentsSpec.setArgs(arguments);
        return this;
    }

    @Nullable
    @Override
    public List<String> getArgs() {
        return argumentsSpec.getArgs();
    }

    @Override
    public List<CommandLineArgumentProvider> getArgumentProviders() {
        return argumentsSpec.getArgumentProviders();
    }

    @Override
    public JavaExecSpec classpath(Object... paths) {
        this.classpath.from(paths);
        return this;
    }

    @Override
    public FileCollection getClasspath() {
        return classpath;
    }

    @Override
    public JavaExecSpec setClasspath(FileCollection classpath) {
        this.classpath = fileCollectionFactory.configurableFiles("classpath");
        this.classpath.setFrom(classpath);
        return this;
    }

    @Override
    public boolean isIgnoreExitValue() {
        return ignoreExitValue;
    }

    @Override
    public JavaExecSpec setIgnoreExitValue(boolean ignoreExitValue) {
        this.ignoreExitValue = ignoreExitValue;
        return this;
    }

    @Override
    public InputStream getStandardInput() {
        return streamsSpec.getStandardInput();
    }

    @Override
    public JavaExecSpec setStandardInput(InputStream standardInput) {
        streamsSpec.setStandardInput(standardInput);
        return this;
    }

    @Override
    public OutputStream getStandardOutput() {
        return streamsSpec.getStandardOutput();
    }

    @Override
    public JavaExecSpec setStandardOutput(OutputStream standardOutput) {
        streamsSpec.setStandardOutput(standardOutput);
        return this;
    }

    @Override
    public OutputStream getErrorOutput() {
        return streamsSpec.getErrorOutput();
    }

    @Override
    public JavaExecSpec setErrorOutput(OutputStream errorOutput) {
        streamsSpec.setErrorOutput(errorOutput);
        return this;
    }

    @Override
    public Property<String> getMainClass() {
        return mainClass;
    }

    @Nullable
    @Override
    @Deprecated
    public String getMain() {
        DeprecationLogger.deprecateMethod(JavaExecSpec.class, "getMain()")
            .withAdvice("Please use the mainClass property instead.")
            .willBeRemovedInGradle8()
            .withUpgradeGuideSection(7, "java_exec_properties")
            .nagUser();

        return mainClass.getOrNull();
    }

    @Override
    @Deprecated
    public JavaExecSpec setMain(@Nullable String main) {
        DeprecationLogger.deprecateMethod(JavaExecSpec.class, "setMain(String)")
            .withAdvice("Please use the mainClass property instead.")
            .willBeRemovedInGradle8()
            .withUpgradeGuideSection(7, "java_exec_properties")
            .nagUser();

        mainClass.set(main);
        return this;
    }

    @Override
    public Property<String> getMainModule() {
        return mainModule;
    }

    @Override
    public ModularitySpec getModularity() {
        return modularity;
    }
}
