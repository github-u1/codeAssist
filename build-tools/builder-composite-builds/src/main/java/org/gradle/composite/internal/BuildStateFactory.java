package org.gradle.composite.internal;

import org.gradle.StartParameter;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.internal.BuildDefinition;
import org.gradle.api.internal.StartParameterInternal;
import org.gradle.initialization.BuildCancellationToken;
import org.gradle.internal.Actions;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.build.PublicBuildPath;
import org.gradle.internal.build.RootBuildState;
import org.gradle.internal.build.StandAloneNestedBuild;
import org.gradle.internal.buildtree.BuildTreeState;
import org.gradle.internal.buildtree.NestedBuildTree;
import org.gradle.internal.enterprise.core.GradleEnterprisePluginManager;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.service.scopes.GradleUserHomeScopeServiceRegistry;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.internal.session.state.CrossBuildSessionState;
import org.gradle.plugin.management.internal.PluginRequests;
import org.gradle.util.Path;

import java.io.File;

import static org.gradle.api.internal.SettingsInternal.BUILD_SRC;

@ServiceScope(Scopes.BuildTree.class)
public class BuildStateFactory {
    private final BuildTreeState buildTreeState;
    private final ListenerManager listenerManager;
    private final GradleEnterprisePluginManager enterprisePluginManager;
    private final GradleUserHomeScopeServiceRegistry userHomeDirServiceRegistry;
    private final CrossBuildSessionState crossBuildSessionState;
    private final BuildCancellationToken buildCancellationToken;

    public BuildStateFactory(
            BuildTreeState buildTreeState,
            ListenerManager listenerManager,
            GradleEnterprisePluginManager enterprisePluginManager,
            GradleUserHomeScopeServiceRegistry userHomeDirServiceRegistry,
            CrossBuildSessionState crossBuildSessionState,
            BuildCancellationToken buildCancellationToken
    ) {
        this.buildTreeState = buildTreeState;
        this.listenerManager = listenerManager;
        this.enterprisePluginManager = enterprisePluginManager;
        this.userHomeDirServiceRegistry = userHomeDirServiceRegistry;
        this.crossBuildSessionState = crossBuildSessionState;
        this.buildCancellationToken = buildCancellationToken;
    }

    public RootBuildState createRootBuild(BuildDefinition buildDefinition) {
        return new DefaultRootBuildState(buildDefinition, buildTreeState, listenerManager, enterprisePluginManager);
    }

    public StandAloneNestedBuild createNestedBuild(BuildIdentifier buildIdentifier, Path identityPath, BuildDefinition buildDefinition, BuildState owner) {
        DefaultNestedBuild build = new DefaultNestedBuild(buildIdentifier, identityPath, buildDefinition, owner, buildTreeState);
        // Expose any contributions from the parent's settings
        build.getMutableModel().setClassLoaderScope(() -> owner.getMutableModel().getSettings().getClassLoaderScope());
        return build;
    }

    public NestedBuildTree createNestedTree(
            BuildDefinition buildDefinition,
            BuildIdentifier buildIdentifier,
            Path identityPath,
            BuildState owner
    ) {
        return new DefaultNestedBuildTree(buildDefinition, buildIdentifier, identityPath, owner, userHomeDirServiceRegistry, crossBuildSessionState, buildCancellationToken);
    }

    public BuildDefinition buildDefinitionFor(File buildSrcDir, BuildState owner) {
        PublicBuildPath publicBuildPath = owner.getMutableModel().getServices().get(PublicBuildPath.class);
        StartParameterInternal buildSrcStartParameter = buildSrcStartParameterFor(buildSrcDir, owner.getMutableModel().getStartParameter());
        BuildDefinition buildDefinition = BuildDefinition.fromStartParameterForBuild(
                buildSrcStartParameter,
                BUILD_SRC,
                buildSrcDir,
                PluginRequests.EMPTY,
                Actions.doNothing(),
                publicBuildPath,
                true
        );
        @SuppressWarnings("deprecation")
        File customBuildFile = buildSrcStartParameter.getBuildFile();
        assert customBuildFile == null;
        return buildDefinition;
    }

    private StartParameterInternal buildSrcStartParameterFor(File buildSrcDir, StartParameter containingBuildParameters) {
        final StartParameterInternal buildSrcStartParameter = (StartParameterInternal) containingBuildParameters.newBuild();
        buildSrcStartParameter.setCurrentDir(buildSrcDir);
        buildSrcStartParameter.setProjectProperties(containingBuildParameters.getProjectProperties());
        buildSrcStartParameter.doNotSearchUpwards();
        buildSrcStartParameter.setProfile(containingBuildParameters.isProfile());
        return buildSrcStartParameter;
    }
}