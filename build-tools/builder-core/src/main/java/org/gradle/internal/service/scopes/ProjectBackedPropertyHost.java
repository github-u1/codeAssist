package org.gradle.internal.service.scopes;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.provider.PropertyHost;
import org.gradle.internal.state.ModelObject;

import org.jetbrains.annotations.Nullable;

class ProjectBackedPropertyHost implements PropertyHost {
    private final ProjectInternal project;

    public ProjectBackedPropertyHost(ProjectInternal project) {
        this.project = project;
    }

    @Nullable
    @Override
    public String beforeRead(@Nullable ModelObject producer) {
        if (!project.getState().hasCompleted()) {
            return "configuration of " + project.getDisplayName() + " has not completed yet";
        } else if (producer != null) {
            TaskInternal producerTask = (TaskInternal) producer.getTaskThatOwnsThisObject();
            if (producerTask != null && producerTask.getState().isConfigurable()) {
                // Currently cannot tell the difference between access from the producing task and access from outside, so assume
                // all access after the task has started execution is ok
                return producerTask + " has not completed yet";
            }
        }
        return null;
    }
}