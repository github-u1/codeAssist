package org.gradle.execution;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Spec;
import org.gradle.execution.taskpath.ResolvedTaskPath;
import org.gradle.execution.taskpath.TaskPathResolver;
import org.gradle.util.NameMatcher;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultTaskSelector extends TaskSelector {
    private static final Logger LOGGER = Logging.getLogger(DefaultTaskSelector.class);

    private final TaskNameResolver taskNameResolver;
    private final GradleInternal gradle;
    private final ProjectConfigurer configurer;
    private final TaskPathResolver taskPathResolver = new TaskPathResolver();

    public DefaultTaskSelector(GradleInternal gradle, TaskNameResolver taskNameResolver, ProjectConfigurer configurer) {
        this.taskNameResolver = taskNameResolver;
        this.gradle = gradle;
        this.configurer = configurer;
    }

    public TaskSelection getSelection(String path) {
        return getSelection(path, gradle.getDefaultProject());
    }

    public Spec<Task> getFilter(String path) {
        final ResolvedTaskPath taskPath = taskPathResolver.resolvePath(path, gradle.getDefaultProject());
        if (!taskPath.isQualified()) {
            ProjectInternal targetProject = taskPath.getProject();
            configurer.configure(targetProject);
            if (taskNameResolver.tryFindUnqualifiedTaskCheaply(taskPath.getTaskName(), taskPath.getProject())) {
                // An exact match in the target project - can just filter tasks by path to avoid configuring sub-projects at this point
                return new TaskPathSpec(targetProject, taskPath.getTaskName());
            }
        }

        final Set<Task> selectedTasks = getSelection(path, gradle.getDefaultProject()).getTasks();
        return new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task element) {
                return !selectedTasks.contains(element);
            }
        };
    }

    public TaskSelection getSelection(@Nullable String projectPath, @Nullable File root, String path) {
        if (root != null) {
            ensureNotFromIncludedBuild(root);
        }

        ProjectInternal project = projectPath != null
                ? gradle.getRootProject().findProject(projectPath)
                : gradle.getDefaultProject();
        return getSelection(path, project);
    }

    private void ensureNotFromIncludedBuild(File root) {
        Set<File> includedRoots = new HashSet<File>();
        for (IncludedBuild includedBuild : gradle.getIncludedBuilds()) {
            includedRoots.add(includedBuild.getProjectDir());
        }
        if (includedRoots.contains(root)) {
            throw new TaskSelectionException("Can't launch tasks from included builds");
        }
    }

    private TaskSelection getSelection(String path, ProjectInternal project) {
        ResolvedTaskPath taskPath = taskPathResolver.resolvePath(path, project);
        ProjectInternal targetProject = taskPath.getProject();
        if (taskPath.isQualified()) {
            configurer.configure(targetProject);
        } else {
            configurer.configureHierarchy(targetProject);
        }

        TaskSelectionResult tasks = taskNameResolver.selectWithName(taskPath.getTaskName(), taskPath.getProject(), !taskPath.isQualified());
        if (tasks != null) {
            LOGGER.info("Task name matched '{}'", taskPath.getTaskName());
            return new TaskSelection(taskPath.getProject().getPath(), path, tasks);
        } else {
            Map<String, TaskSelectionResult> tasksByName = taskNameResolver.selectAll(taskPath.getProject(), !taskPath.isQualified());
            NameMatcher matcher = new NameMatcher();
            String actualName = matcher.find(taskPath.getTaskName(), tasksByName.keySet());

            if (actualName != null) {
                LOGGER.info("Abbreviated task name '{}' matched '{}'", taskPath.getTaskName(), actualName);
                return new TaskSelection(taskPath.getProject().getPath(), taskPath.getPrefix() + actualName, tasksByName.get(actualName));
            }

            throw new TaskSelectionException(matcher.formatErrorMessage("task", taskPath.getProject()));
        }
    }

    private static class TaskPathSpec implements Spec<Task> {
        private final ProjectInternal targetProject;
        private final String taskName;

        public TaskPathSpec(ProjectInternal targetProject, String taskName) {
            this.targetProject = targetProject;
            this.taskName = taskName;
        }

        @Override
        public boolean isSatisfiedBy(Task element) {
            if (!element.getName().equals(taskName)) {
                return true;
            }
            for (Project current = element.getProject(); current != null; current = current.getParent()) {
                if (current.equals(targetProject)) {
                    return false;
                }
            }
            return true;
        }
    }
}