/*
 * Copyright 2019 the original author or authors.
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

package com.tyron.builder.internal.component.external.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tyron.builder.api.artifacts.component.ModuleComponentIdentifier;
import com.tyron.builder.api.capabilities.CapabilitiesMetadata;
import com.tyron.builder.api.internal.attributes.ImmutableAttributes;
import com.tyron.builder.internal.Describables;
import com.tyron.builder.internal.DisplayName;
import com.tyron.builder.internal.component.model.ComponentArtifactMetadata;
import com.tyron.builder.internal.component.model.DefaultVariantMetadata;
import com.tyron.builder.internal.component.model.ExcludeMetadata;
import com.tyron.builder.internal.component.model.IvyArtifactName;
import com.tyron.builder.internal.component.model.ModuleConfigurationMetadata;
import com.tyron.builder.internal.component.model.VariantResolveMetadata;
import com.tyron.builder.internal.deprecation.DeprecationMessageBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * A configuration representing an additional variant of a published component added by a component metadata rule.
 * It can be backed by an existing configuration/variant (base) or can initially be empty (base = null).
 */
class LazyRuleAwareWithBaseConfigurationMetadata implements ModuleConfigurationMetadata {

    private final String name;
    private final ModuleConfigurationMetadata base;
    private final ModuleComponentIdentifier componentId;
    private final VariantMetadataRules variantMetadataRules;
    private final ImmutableList<ExcludeMetadata> excludes;

    private List<? extends ModuleDependencyMetadata> computedDependencies;
    private ImmutableAttributes computedAttributes;
    private CapabilitiesMetadata computedCapabilities;
    private ImmutableList<? extends ComponentArtifactMetadata> computedArtifacts;
    private final ImmutableAttributes componentLevelAttributes;
    private final boolean externalVariant;

    LazyRuleAwareWithBaseConfigurationMetadata(String name,
                                               @Nullable ModuleConfigurationMetadata base,
                                               ModuleComponentIdentifier componentId,
                                               ImmutableAttributes componentLevelAttributes,
                                               VariantMetadataRules variantMetadataRules,
                                               ImmutableList<ExcludeMetadata> excludes,
                                               boolean externalVariant) {
        this.name = name;
        this.base = base;
        this.componentId = componentId;
        this.variantMetadataRules = variantMetadataRules;
        this.componentLevelAttributes = componentLevelAttributes;
        this.excludes = excludes;
        this.externalVariant = externalVariant;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Identifier getIdentifier() {
        return null;
    }

    @Override
    public List<? extends ModuleDependencyMetadata> getDependencies() {
        if (computedDependencies == null) {
            computedDependencies = variantMetadataRules.applyDependencyMetadataRules(this, base == null ? ImmutableList.of() : base.getDependencies());
        }
        return computedDependencies;
    }

    @Override
    public ImmutableAttributes getAttributes() {
        if (computedAttributes == null) {
            computedAttributes = variantMetadataRules.applyVariantAttributeRules(this, base == null ? componentLevelAttributes : base.getAttributes());
        }
        return computedAttributes;
    }

    @Override
    public ImmutableList<? extends ComponentArtifactMetadata> getArtifacts() {
        if (computedArtifacts == null) {
            computedArtifacts = variantMetadataRules.applyVariantFilesMetadataRulesToArtifacts(this, base == null ? ImmutableList.of() : base.getArtifacts(), componentId);
        }
        return computedArtifacts;
    }

    @Override
    public CapabilitiesMetadata getCapabilities() {
        if (computedCapabilities == null) {
            computedCapabilities = variantMetadataRules.applyCapabilitiesRules(this, base == null ? ImmutableCapabilities.EMPTY : base.getCapabilities());
        }
        return computedCapabilities;
    }

    @Override
    public boolean requiresMavenArtifactDiscovery() {
        return false;
    }

    @Override
    public Set<? extends VariantResolveMetadata> getVariants() {
        return ImmutableSet.of(new DefaultVariantMetadata(name, null, asDescribable(), getAttributes(), getArtifacts(), getCapabilities()));
    }

    @Override
    public DisplayName asDescribable() {
        return Describables.of(componentId, "configuration", name);
    }

    @Override
    public ComponentArtifactMetadata artifact(IvyArtifactName artifact) {
        return new DefaultModuleComponentArtifactMetadata(componentId, artifact);
    }

    @Override
    public ImmutableSet<String> getHierarchy() {
        return ImmutableSet.of(name);
    }

    @Override
    public ImmutableList<ExcludeMetadata> getExcludes() {
        return excludes;
    }

    @Override
    public boolean isTransitive() {
        return true;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isCanBeConsumed() {
        return true;
    }

    @Override
    public boolean isCanBeResolved() {
        return false;
    }

    @Override
    public DeprecationMessageBuilder.WithDocumentation getConsumptionDeprecation() {
        return null;
    }

    @Override
    public boolean isExternalVariant() {
        return externalVariant;
    }
}