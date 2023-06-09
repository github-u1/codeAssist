package org.gradle.api.internal.tasks.compile;

import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;
import org.gradle.internal.jvm.Jvm;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecHandle;
import org.gradle.process.internal.ExecHandleBuilder;
import org.gradle.process.internal.ExecHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Executes the Java command line compiler specified in {@code JavaCompileSpec.forkOptions.getExecutable()}.
 */
public class CommandLineJavaCompiler implements Compiler<JavaCompileSpec>, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineJavaCompiler.class);

    private final CompileSpecToArguments<JavaCompileSpec> argumentsGenerator = new CommandLineJavaCompilerArgumentsGenerator();
    private final ExecHandleFactory execHandleFactory;

    public CommandLineJavaCompiler(ExecHandleFactory execHandleFactory) {
        this.execHandleFactory = execHandleFactory;
    }

    @Override
    public WorkResult execute(JavaCompileSpec spec) {
        final MinimalJavaCompilerDaemonForkOptions forkOptions = spec.getCompileOptions().getForkOptions();
        String executable = forkOptions.getJavaHome() != null ? Jvm.forHome(forkOptions.getJavaHome()).getJavacExecutable().getAbsolutePath() : forkOptions.getExecutable();
        LOGGER.info("Compiling with Java command line compiler '{}'.", executable);

        ExecHandle handle = createCompilerHandle(executable, spec);
        executeCompiler(handle);

        return WorkResults.didWork(true);
    }

    private ExecHandle createCompilerHandle(String executable, JavaCompileSpec spec) {
        ExecHandleBuilder builder = execHandleFactory.newExec();
        builder.setWorkingDir(spec.getWorkingDir());
        builder.setExecutable(executable);
        argumentsGenerator.collectArguments(spec, new ExecSpecBackedArgCollector(builder));
        builder.setIgnoreExitValue(true);
        return builder.build();
    }

    private void executeCompiler(ExecHandle handle) {
        handle.start();
        ExecResult result = handle.waitForFinish();
        if (result.getExitValue() != 0) {
            throw new CompilationFailedException(result.getExitValue());
        }
    }
}
