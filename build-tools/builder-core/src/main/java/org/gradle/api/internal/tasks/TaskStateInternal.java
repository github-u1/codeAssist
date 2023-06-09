package org.gradle.api.internal.tasks;

import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.TaskState;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TaskStateInternal implements TaskState {
    private boolean executing;
    private boolean actionable = true;
    private boolean didWork;
    private RuntimeException failure;
    private TaskExecutionOutcome outcome;

    @Override
    public boolean getDidWork() {
        return didWork;
    }

    public void setDidWork(boolean didWork) {
        this.didWork = didWork;
    }

    @Override
    public boolean getExecuted() {
        return outcome != null;
    }

    public boolean isConfigurable() {
        return !getExecuted() && !executing;
    }

    @Nullable
    public TaskExecutionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(TaskExecutionOutcome outcome) {
        assert this.outcome == null;
        this.outcome = outcome;
    }

    /**
     * Marks this task as executed with the given failure. This method can be called at most once.
     */
    public void setOutcome(RuntimeException failure) {
        assert this.failure == null;
        this.outcome = TaskExecutionOutcome.EXECUTED;
        this.failure = failure;
    }

    public void addFailure(TaskExecutionException failure) {
        if (this.failure == null) {
            this.failure = failure;
        } else if (this.failure instanceof TaskExecutionException) {
            TaskExecutionException taskExecutionException = (TaskExecutionException) this.failure;
            List<Throwable> causes = new ArrayList<>(taskExecutionException.getCauses());
            causes.addAll(failure.getCauses());
            taskExecutionException.initCauses(causes);
        } else {
            List<Throwable> causes = new ArrayList<>();
            causes.add(this.failure);
            causes.addAll(failure.getCauses());
            failure.initCauses(causes);
            this.failure = failure;
        }
    }

    public boolean getExecuting() {
        return executing;
    }

    public void setExecuting(boolean executing) {
        this.executing = executing;
    }

    @Override
    public Throwable getFailure() {
        return failure;
    }

    @Override
    public void rethrowFailure() {
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public boolean getSkipped() {
        return outcome != null && outcome.isSkipped();
    }

    @Override
    public String getSkipMessage() {
        return outcome != null ? outcome.getMessage() : null;
    }

    @Override
    public boolean getUpToDate() {
        return outcome != null && outcome.isUpToDate();
    }

    @Override
    public boolean getNoSource() {
        return outcome == TaskExecutionOutcome.NO_SOURCE;
    }

    public boolean isFromCache() {
        return outcome == TaskExecutionOutcome.FROM_CACHE;
    }

    public boolean isActionable() {
        return actionable;
    }

    public void setActionable(boolean actionable) {
        this.actionable = actionable;
    }
}