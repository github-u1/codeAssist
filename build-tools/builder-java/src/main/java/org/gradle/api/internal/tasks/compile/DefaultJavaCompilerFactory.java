package org.gradle.api.internal.tasks.compile;

import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.tasks.compile.processing.AnnotationProcessorDetector;
import org.gradle.internal.Factory;
import org.gradle.jvm.toolchain.internal.JavaCompilerFactory;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.internal.ExecHandleFactory;
import org.gradle.process.internal.JavaForkOptionsFactory;
import org.gradle.process.internal.worker.child.WorkerDirectoryProvider;
import org.gradle.workers.internal.ActionExecutionSpecFactory;
import org.gradle.workers.internal.WorkerDaemonFactory;

import javax.tools.JavaCompiler;

public class DefaultJavaCompilerFactory implements JavaCompilerFactory {
    private final WorkerDirectoryProvider workingDirProvider;
    private final WorkerDaemonFactory workerDaemonFactory;
    private final JavaForkOptionsFactory forkOptionsFactory;
    private final ExecHandleFactory execHandleFactory;
    private final AnnotationProcessorDetector processorDetector;
    private final ClassPathRegistry classPathRegistry;
    private final ActionExecutionSpecFactory actionExecutionSpecFactory;
    private Factory<JavaCompiler> javaHomeBasedJavaCompilerFactory;

    public DefaultJavaCompilerFactory(WorkerDirectoryProvider workingDirProvider, WorkerDaemonFactory workerDaemonFactory, JavaForkOptionsFactory forkOptionsFactory, ExecHandleFactory execHandleFactory, AnnotationProcessorDetector processorDetector, ClassPathRegistry classPathRegistry, ActionExecutionSpecFactory actionExecutionSpecFactory) {
        this.workingDirProvider = workingDirProvider;
        this.workerDaemonFactory = workerDaemonFactory;
        this.forkOptionsFactory = forkOptionsFactory;
        this.execHandleFactory = execHandleFactory;
        this.processorDetector = processorDetector;
        this.classPathRegistry = classPathRegistry;
        this.actionExecutionSpecFactory = actionExecutionSpecFactory;
    }

    private Factory<JavaCompiler> getJavaHomeBasedJavaCompilerFactory() {
        if (javaHomeBasedJavaCompilerFactory == null) {
            javaHomeBasedJavaCompilerFactory = new JavaHomeBasedJavaCompilerFactory(classPathRegistry.getClassPath("JAVA-COMPILER-PLUGIN").getAsFiles());
        }
        return javaHomeBasedJavaCompilerFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CompileSpec> Compiler<T> create(Class<T> type) {
        Compiler<T> result = createTargetCompiler(type);
        return (Compiler<T>) new ModuleApplicationNameWritingCompiler<>(new AnnotationProcessorDiscoveringCompiler<>(new NormalizingJavaCompiler((Compiler<JavaCompileSpec>) result), processorDetector));
    }

    @SuppressWarnings("unchecked")
    private <T extends CompileSpec> Compiler<T> createTargetCompiler(Class<T> type) {
        if (!JavaCompileSpec.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format("Cannot create a compiler for a spec with type %s", type.getSimpleName()));
        }

        if (CommandLineJavaCompileSpec.class.isAssignableFrom(type)) {
            return (Compiler<T>) new CommandLineJavaCompiler(execHandleFactory);
        }

        if (ForkingJavaCompileSpec.class.isAssignableFrom(type)) {
            return (Compiler<T>) new DaemonJavaCompiler(workingDirProvider.getWorkingDirectory(), JdkJavaCompiler.class, new Object[]{getJavaHomeBasedJavaCompilerFactory()}, workerDaemonFactory, forkOptionsFactory, classPathRegistry, actionExecutionSpecFactory);
        } else {
            return (Compiler<T>) new JdkJavaCompiler(getJavaHomeBasedJavaCompilerFactory());
        }
    }
}
