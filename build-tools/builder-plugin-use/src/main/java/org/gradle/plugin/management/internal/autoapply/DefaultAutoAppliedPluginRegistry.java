/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugin.management.internal.autoapply;

import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.BuildDefinition;
import org.gradle.api.internal.StartParameterInternal;
import org.gradle.plugin.management.internal.PluginRequestInternal;
import org.gradle.plugin.management.internal.PluginRequests;

/**
 * A hardcoded {@link AutoAppliedPluginRegistry} that only knows about the build-scan plugin for now.
 */
public class DefaultAutoAppliedPluginRegistry implements AutoAppliedPluginRegistry {

//    private static final PluginRequests GRADLE_ENTERPRISE_PLUGIN_REQUEST = PluginRequests.of(createGradleEnterprisePluginRequest());

    private final BuildDefinition buildDefinition;

    public DefaultAutoAppliedPluginRegistry(BuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    @Override
    public PluginRequests getAutoAppliedPlugins(Project target) {
        return PluginRequests.EMPTY;
    }

    @Override
    public PluginRequests getAutoAppliedPlugins(Settings target) {
        if (((StartParameterInternal) target.getStartParameter()).isUseEmptySettings()) {
            return PluginRequests.EMPTY;
        }

        PluginRequests injectedPluginRequests = buildDefinition.getInjectedPluginRequests();

        if (shouldApplyGradleEnterprisePlugin(target)) {
            throw new UnsupportedOperationException();
//            return injectedPluginRequests.mergeWith(GRADLE_ENTERPRISE_PLUGIN_REQUEST);
        } else {
            return injectedPluginRequests;
        }
    }

    private boolean shouldApplyGradleEnterprisePlugin(Settings settings) {
//        Gradle gradle = settings.getGradle();
//        StartParameter startParameter = gradle.getStartParameter();
//        return startParameter.isBuildScan() && gradle.getParent() == null;
        return false;
    }

    private static PluginRequestInternal createGradleEnterprisePluginRequest() {
//        ModuleIdentifier moduleIdentifier = DefaultModuleIdentifier.newId(AutoAppliedGradleEnterprisePlugin.GROUP, AutoAppliedGradleEnterprisePlugin.NAME);
//        ModuleVersionSelector artifact = DefaultModuleVersionSelector.newSelector(moduleIdentifier, AutoAppliedGradleEnterprisePlugin.VERSION);
//        return new DefaultPluginRequest(AutoAppliedGradleEnterprisePlugin.ID, AutoAppliedGradleEnterprisePlugin.VERSION, true, null, getScriptDisplayName(), artifact);
        throw new UnsupportedOperationException();
    }

    private static String getScriptDisplayName() {
        return String.format("auto-applied by using --%s", "Build scan long option");
    }
}
