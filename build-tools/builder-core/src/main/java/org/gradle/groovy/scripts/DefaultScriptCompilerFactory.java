package org.gradle.groovy.scripts;

import org.gradle.api.Action;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.groovy.scripts.internal.CompileOperation;
import org.gradle.groovy.scripts.internal.CompiledScript;
import org.gradle.groovy.scripts.internal.ScriptClassCompiler;
import org.gradle.groovy.scripts.internal.ScriptRunnerFactory;

import org.codehaus.groovy.ast.ClassNode;

public class DefaultScriptCompilerFactory implements ScriptCompilerFactory {
    private final ScriptRunnerFactory scriptRunnerFactory;
    private final ScriptClassCompiler scriptClassCompiler;

    public DefaultScriptCompilerFactory(ScriptClassCompiler scriptClassCompiler, ScriptRunnerFactory scriptRunnerFactory) {
        this.scriptClassCompiler = scriptClassCompiler;
        this.scriptRunnerFactory = scriptRunnerFactory;
    }

    @Override
    public ScriptCompiler createCompiler(ScriptSource source) {
        return new ScriptCompilerImpl(source);
    }

    private class ScriptCompilerImpl implements ScriptCompiler {
        private final ScriptSource source;

        public ScriptCompilerImpl(ScriptSource source) {
            this.source = CachingScriptSource.of(source);
        }

        @Override
        public <T extends Script, M> ScriptRunner<T, M> compile(Class<T> scriptType, CompileOperation<M> extractingTransformer, ClassLoaderScope targetScope, Action<? super ClassNode> verifier) {
            CompiledScript<T, M> compiledScript = scriptClassCompiler.compile(source, targetScope, extractingTransformer, scriptType, verifier);
            return scriptRunnerFactory.create(compiledScript, source, targetScope.getExportClassLoader());
        }
    }
}
