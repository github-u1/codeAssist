package org.gradle.api.internal.tasks.compile;

import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.tasks.compile.daemon.AbstractDaemonCompiler;
import org.gradle.internal.classloader.VisitableURLClassLoader;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.jvm.Jvm;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.internal.JavaForkOptionsFactory;
import org.gradle.workers.internal.ActionExecutionSpecFactory;
import org.gradle.workers.internal.DaemonForkOptions;
import org.gradle.workers.internal.DaemonForkOptionsBuilder;
import org.gradle.workers.internal.FlatClassLoaderStructure;
import org.gradle.workers.internal.KeepAliveMode;
import org.gradle.workers.internal.WorkerDaemonFactory;

import java.io.File;

public class DaemonJavaCompiler extends AbstractDaemonCompiler<JavaCompileSpec> {
    private final Class<? extends Compiler<JavaCompileSpec>> compilerClass;
    private final Object[] compilerConstructorArguments;
    private final JavaForkOptionsFactory forkOptionsFactory;
    private final File daemonWorkingDir;
    private final ClassPathRegistry classPathRegistry;

    public DaemonJavaCompiler(File daemonWorkingDir, Class<? extends Compiler<JavaCompileSpec>> compilerClass, Object[] compilerConstructorArguments, WorkerDaemonFactory workerDaemonFactory, JavaForkOptionsFactory forkOptionsFactory, ClassPathRegistry classPathRegistry, ActionExecutionSpecFactory actionExecutionSpecFactory) {
        super(workerDaemonFactory, actionExecutionSpecFactory);
        this.compilerClass = compilerClass;
        this.compilerConstructorArguments = compilerConstructorArguments;
        this.forkOptionsFactory = forkOptionsFactory;
        this.daemonWorkingDir = daemonWorkingDir;
        this.classPathRegistry = classPathRegistry;
    }

    @Override
    protected CompilerParameters getCompilerParameters(JavaCompileSpec spec) {
        return new JavaCompilerParameters(compilerClass.getName(), compilerConstructorArguments, spec);
    }

    @Override
    protected DaemonForkOptions toDaemonForkOptions(JavaCompileSpec spec) {
        MinimalJavaCompilerDaemonForkOptions forkOptions = spec.getCompileOptions().getForkOptions();
        JavaForkOptions javaForkOptions = new BaseForkOptionsConverter(forkOptionsFactory).transform(forkOptions);
        javaForkOptions.setWorkingDir(daemonWorkingDir);
        javaForkOptions.setExecutable(findSuitableExecutable(spec));

        ClassPath compilerClasspath = classPathRegistry.getClassPath("JAVA-COMPILER");
        FlatClassLoaderStructure classLoaderStructure = new FlatClassLoaderStructure(new VisitableURLClassLoader.Spec("compiler", compilerClasspath.getAsURLs()));

        return new DaemonForkOptionsBuilder(forkOptionsFactory)
            .javaForkOptions(javaForkOptions)
            .withClassLoaderStructure(classLoaderStructure)
            .keepAliveMode(KeepAliveMode.SESSION)
            .build();
    }

    private File findSuitableExecutable(JavaCompileSpec spec) {
        final MinimalJavaCompilerDaemonForkOptions forkOptions = spec.getCompileOptions().getForkOptions();
        if (forkOptions.getExecutable() != null) {
            return new File(forkOptions.getExecutable());
        } else if (forkOptions.getJavaHome() != null) {
            return Jvm.forHome(forkOptions.getJavaHome()).getJavaExecutable();
        }
        return Jvm.current().getJavaExecutable();
    }

    public static class JavaCompilerParameters extends CompilerParameters {
        private final JavaCompileSpec compileSpec;

        public JavaCompilerParameters(String compilerClassName, Object[] compilerInstanceParameters, JavaCompileSpec compileSpec) {
            super(compilerClassName, compilerInstanceParameters);
            this.compileSpec = compileSpec;
        }

        @Override
        public JavaCompileSpec getCompileSpec() {
            return compileSpec;
        }
    }
}
