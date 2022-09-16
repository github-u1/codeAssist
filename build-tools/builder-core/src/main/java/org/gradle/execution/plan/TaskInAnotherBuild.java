package org.gradle.execution.plan;

import org.gradle.api.Action;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.NodeExecutionContext;
import org.gradle.composite.internal.BuildTreeWorkGraphController;
import org.gradle.composite.internal.IncludedBuildTaskResource;
import org.gradle.composite.internal.TaskIdentifier;
import org.gradle.internal.lazy.Lazy;
import org.gradle.internal.resources.ResourceLock;
import org.gradle.util.Path;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class TaskInAnotherBuild extends TaskNode implements SelfExecutingNode {
    public static TaskInAnotherBuild of(
            TaskInternal task,
            BuildTreeWorkGraphController taskGraph
    ) {
        BuildIdentifier targetBuild = buildIdentifierOf(task);
        TaskIdentifier taskIdentifier = TaskIdentifier.of(targetBuild, task);
        IncludedBuildTaskResource taskResource = taskGraph.locateTask(taskIdentifier);
        return new TaskInAnotherBuild(task.getIdentityPath(), task.getPath(), targetBuild) {
            @Override
            protected IncludedBuildTaskResource getTarget() {
                return taskResource;
            }
        };
    }

    /**
     * Creates a lazy reference to a task in another build.
     *
     * The task will be located on-demand to allow for cycles between builds stored to
     * the configuration cache.
     *
     * @param taskPath the path to the task relative to its build
     * @param targetBuild the build containing the task
     * @param taskGraph the task graph where the task should be located
     * @return a lazy reference to the given task.
     */
    public static TaskInAnotherBuild lazy(
            String taskPath,
            BuildIdentifier targetBuild,
            BuildTreeWorkGraphController taskGraph
    ) {
        TaskIdentifier taskIdentifier = TaskIdentifier.of(targetBuild, taskPath);
        Path taskIdentityPath = Path.path(targetBuild.getName()).append(Path.path(taskPath));
        Lazy<IncludedBuildTaskResource> target = Lazy.unsafe().of(() -> taskGraph.locateTask(taskIdentifier));
        return new TaskInAnotherBuild(taskIdentityPath, taskPath, targetBuild) {
            @Override
            protected IncludedBuildTaskResource getTarget() {
                return target.get();
            }
        };
    }

    private IncludedBuildTaskResource.State taskState = IncludedBuildTaskResource.State.Waiting;
    private final Path taskIdentityPath;
    private final String taskPath;
    private final BuildIdentifier targetBuild;

    protected TaskInAnotherBuild(Path taskIdentityPath, String taskPath, BuildIdentifier targetBuild) {
        this.taskIdentityPath = taskIdentityPath;
        this.taskPath = taskPath;
        this.targetBuild = targetBuild;
    }

    public BuildIdentifier getTargetBuild() {
        return targetBuild;
    }

    public String getTaskPath() {
        return taskPath;
    }

    public Path getTaskIdentityPath() {
        return taskIdentityPath;
    }

    @Override
    public TaskInternal getTask() {
        return getTarget().getTask();
    }

    @Override
    public Set<Node> getLifecycleSuccessors() {
        return Collections.emptySet();
    }

    @Override
    public void setLifecycleSuccessors(Set<Node> successors) {
        if (!successors.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void prepareForExecution(Action<Node> monitor) {
        IncludedBuildTaskResource target = getTarget();
        target.queueForExecution();
        target.onComplete(() -> monitor.execute(this));
    }

    @Nullable
    @Override
    public ResourceLock getProjectToLock() {
        // Ignore, as the node in the other build's execution graph takes care of this
        return null;
    }

    @Nullable
    @Override
    public ProjectInternal getOwningProject() {
        // Ignore, as the node in the other build's execution graph takes care of this
        return null;
    }

    @Override
    public List<ResourceLock> getResourcesToLock() {
        // Ignore, as the node in the other build's execution graph will take care of this
        return Collections.emptyList();
    }

    @Override
    public Throwable getNodeFailure() {
        return null;
    }

    @Override
    public void resolveDependencies(TaskDependencyResolver dependencyResolver) {
    }

    @Override
    public DependenciesState doCheckDependenciesComplete() {
        DependenciesState dependenciesState = super.doCheckDependenciesComplete();
        if (dependenciesState != DependenciesState.COMPLETE_AND_SUCCESSFUL) {
            return dependenciesState;
        }

        // This node is ready to "execute" when the task in the other build has completed
        if (!taskState.isComplete()) {
            taskState = getTarget().getTaskState();
        }
        switch (taskState) {
            case Waiting:
                return DependenciesState.NOT_COMPLETE;
            case Success:
                return DependenciesState.COMPLETE_AND_SUCCESSFUL;
            case Failed:
                return DependenciesState.COMPLETE_AND_NOT_SUCCESSFUL;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return "other build task " + taskIdentityPath;
    }

    @Override
    protected void nodeSpecificHealthDiagnostics(StringBuilder builder) {
        builder.append(", taskState=").append(taskState).append(", ").append(getTarget().healthDiagnostics());
    }

    @Override
    public void execute(NodeExecutionContext context) {
        // This node does not do anything itself
    }

    private static BuildIdentifier buildIdentifierOf(TaskInternal task) {
        return ((ProjectInternal) task.getProject()).getOwner().getOwner().getBuildIdentifier();
    }

    protected abstract IncludedBuildTaskResource getTarget();
}