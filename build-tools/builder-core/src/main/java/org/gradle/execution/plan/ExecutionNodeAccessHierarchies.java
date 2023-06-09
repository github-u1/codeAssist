package org.gradle.execution.plan;

import org.gradle.execution.ProjectExecutionServices;
import org.gradle.internal.file.Stat;
import org.gradle.internal.snapshot.CaseSensitivity;

public class ExecutionNodeAccessHierarchies {
    private final ExecutionNodeAccessHierarchy outputHierarchy;
    private final ExecutionNodeAccessHierarchy destroyableHierarchy;
    private final CaseSensitivity caseSensitivity;
    private final Stat stat;

    public ExecutionNodeAccessHierarchies(CaseSensitivity caseSensitivity, Stat stat) {
        this.caseSensitivity = caseSensitivity;
        this.stat = stat;
        outputHierarchy = new ExecutionNodeAccessHierarchy(caseSensitivity, stat);
        destroyableHierarchy = new ExecutionNodeAccessHierarchy(caseSensitivity, stat);
    }

    /**
     * Create the input node access hierarchy.
     *
     * For performance reasons, we keep one input node access hierarchy per project,
     * so we only have a factory method here and this is used to create the hierarchy in {@link ProjectExecutionServices}.
     */
    public InputNodeAccessHierarchy createInputHierarchy() {
        return new InputNodeAccessHierarchy(caseSensitivity, stat);
    }

    public ExecutionNodeAccessHierarchy getOutputHierarchy() {
        return outputHierarchy;
    }

    public ExecutionNodeAccessHierarchy getDestroyableHierarchy() {
        return destroyableHierarchy;
    }

    public static class InputNodeAccessHierarchy extends ExecutionNodeAccessHierarchy {
        public InputNodeAccessHierarchy(CaseSensitivity caseSensitivity, Stat stat) {
            super(caseSensitivity, stat);
        }
    }
}