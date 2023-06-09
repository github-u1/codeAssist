package org.gradle.groovy.scripts.internal;

import org.gradle.internal.UncheckedException;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Excludes everything from a script except statements that are satisfied by a given predicate, and imports for accessible classes.
 * <p>
 * All other kinds of constructs are filtered, including classes and methods etc.
 */
public class SubsetScriptTransformer extends AbstractScriptTransformer {

    private final StatementTransformer transformer;

    public SubsetScriptTransformer(StatementTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    protected int getPhase() {
        return Phases.CONVERSION;
    }

    @Override
    public void call(SourceUnit source) throws CompilationFailedException {
        AstUtils.filterAndTransformStatements(source, transformer);

        // Filter imported classes which are not available yet

        Iterator<ImportNode> iter = source.getAST().getImports().iterator();
        while (iter.hasNext()) {
            ImportNode importedClass = iter.next();
            if (!AstUtils.isVisible(source, importedClass.getClassName())) {
                try {
                    Field field = ModuleNode.class.getDeclaredField("imports");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked") List<ImportNode> value = (List<ImportNode>) field.get(source.getAST());
                    value.removeIf(i -> i.getAlias().equals(importedClass.getAlias()));
                } catch (Exception e) {
                    throw UncheckedException.throwAsUncheckedException(e);
                }
            }
        }

        iter = source.getAST().getStaticImports().values().iterator();
        while (iter.hasNext()) {
            ImportNode importedClass = iter.next();
            if (!AstUtils.isVisible(source, importedClass.getClassName())) {
                iter.remove();
            }
        }

        iter = source.getAST().getStaticStarImports().values().iterator();
        while (iter.hasNext()) {
            ImportNode importedClass = iter.next();
            if (!AstUtils.isVisible(source, importedClass.getClassName())) {
                iter.remove();
            }
        }

        ClassNode scriptClass = AstUtils.getScriptClass(source);

        // Remove all the classes other than the main class
        source.getAST().getClasses().removeIf(classNode -> classNode != scriptClass);

        // Remove all the methods from the main class
        if (scriptClass != null) {
            for (MethodNode methodNode : new ArrayList<>(scriptClass.getMethods())) {
                if (!methodNode.getName().equals("run")) {
                    AstUtils.removeMethod(scriptClass, methodNode);
                }
            }
        }

        source.getAST().getMethods().clear();
    }

}
