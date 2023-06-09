package org.gradle.api.internal.project;

import org.gradle.api.Action;
import org.gradle.internal.Actions;
import org.gradle.api.internal.DefaultMutationGuard;
import org.gradle.api.internal.MutationGuard;
import org.gradle.api.internal.WithMutationGuard;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.api.Project;

import java.util.Collections;

public class BuildOperationCrossProjectConfigurator implements CrossProjectConfigurator, WithMutationGuard {

    private final BuildOperationExecutor buildOperationExecutor;
    private final MutationGuard mutationGuard = new DefaultMutationGuard();

    public BuildOperationCrossProjectConfigurator(BuildOperationExecutor buildOperationExecutor) {
        this.buildOperationExecutor = buildOperationExecutor;
    }

    @Override
    public void project(ProjectInternal project, Action<? super Project> configureAction) {
        runProjectConfigureAction(project, configureAction);
    }

    @Override
    public void subprojects(Iterable<? extends ProjectInternal> projects, Action<? super Project> configureAction) {
        runBlockConfigureAction(SUBPROJECTS_DETAILS, projects, configureAction);
    }

    @Override
    public void allprojects(Iterable<? extends ProjectInternal> projects, Action<? super Project> configureAction) {
        runBlockConfigureAction(ALLPROJECTS_DETAILS, projects, configureAction);
    }

    @Override
    public void rootProject(ProjectInternal project, Action<? super Project> buildOperationExecutor) {
        runBlockConfigureAction(ROOT_PROJECT_DETAILS, Collections.singleton(project), buildOperationExecutor);
    }

    private void runBlockConfigureAction(final BuildOperationDescriptor.Builder details, final Iterable<? extends ProjectInternal> projects, final Action<? super Project> configureAction) {
        buildOperationExecutor.run(new BlockConfigureBuildOperation(details, projects, configureAction));
    }

    private void runProjectConfigureAction(final ProjectInternal project, final Action<? super Project> configureAction) {
        project.getOwner().applyToMutableState(p -> buildOperationExecutor.run(new CrossConfigureProjectBuildOperation(project) {
            @Override
            public void run(BuildOperationContext context) {
                Actions.with(project, mutationGuard.withMutationEnabled(configureAction));
            }
        }));
    }

    @Override
    public MutationGuard getMutationGuard() {
        return mutationGuard;
    }

    private final static String ALLPROJECTS = "allprojects";
    private final static String SUBPROJECTS = "subprojects";
    private final static String ROOTPROJECT = "rootProject";

    private final static BuildOperationDescriptor.Builder ALLPROJECTS_DETAILS = computeConfigurationBlockBuildOperationDetails(ALLPROJECTS);
    private final static BuildOperationDescriptor.Builder SUBPROJECTS_DETAILS = computeConfigurationBlockBuildOperationDetails(SUBPROJECTS);
    private final static BuildOperationDescriptor.Builder ROOT_PROJECT_DETAILS = computeConfigurationBlockBuildOperationDetails(ROOTPROJECT);

    private static BuildOperationDescriptor.Builder computeConfigurationBlockBuildOperationDetails(String configurationBlockName) {
        return BuildOperationDescriptor.displayName("Execute '" + configurationBlockName + " {}' action").name(configurationBlockName);
    }

    private class BlockConfigureBuildOperation implements RunnableBuildOperation {

        private final BuildOperationDescriptor.Builder details;
        private final Iterable<? extends ProjectInternal> projects;
        private final Action<? super Project> configureAction;

        private BlockConfigureBuildOperation(BuildOperationDescriptor.Builder details, Iterable<? extends ProjectInternal> projects, Action<? super Project> configureAction) {
            this.details = details;
            this.projects = projects;
            this.configureAction = configureAction;
        }

        @Override
        public BuildOperationDescriptor.Builder description() {
            return details;
        }

        @Override
        public void run(BuildOperationContext context) {
            for (ProjectInternal project : projects) {
                runProjectConfigureAction(project, configureAction);
            }
        }
    }

    private static abstract class CrossConfigureProjectBuildOperation implements RunnableBuildOperation {
        private final Project project;

        private CrossConfigureProjectBuildOperation(Project project) {
            this.project = project;
        }

        @Override
        public BuildOperationDescriptor.Builder description() {
            String name = "Cross-configure project " + ((ProjectInternal) project).getIdentityPath().toString();
            return BuildOperationDescriptor.displayName(name);
        }
    }
}
