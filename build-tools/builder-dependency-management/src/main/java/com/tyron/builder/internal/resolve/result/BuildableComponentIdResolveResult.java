/*
 * Copyright 2014 the original author or authors.
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

package com.tyron.builder.internal.resolve.result;

import com.tyron.builder.internal.component.model.ComponentResolveMetadata;
import com.tyron.builder.internal.resolve.ModuleVersionResolveException;
import com.tyron.builder.internal.resolve.RejectedVersion;

import com.tyron.builder.api.artifacts.ModuleVersionIdentifier;
import com.tyron.builder.api.artifacts.component.ComponentIdentifier;

import java.util.Collection;

public interface BuildableComponentIdResolveResult extends ComponentIdResolveResult, ResourceAwareResolveResult {
    /**
     * Marks the component selector as resolved to the specified id.
     */
    void resolved(ComponentIdentifier id, ModuleVersionIdentifier moduleVersionIdentifier);

    /**
     * Marks the component selector as resolved to the specified id, but rejected.
     */
    void rejected(ComponentIdentifier id, ModuleVersionIdentifier moduleVersionIdentifier);

    /**
     * Marks the component selector as resolved, with the provided metadata. The id is taken from the metadata.
     */
    void resolved(ComponentResolveMetadata metaData);

    /**
     * Marks the component selection as failed.
     */
    void failed(ModuleVersionResolveException failure);

    /**
     * Registers the list of versions that were attempted for this module, but didn't match
     * the selector. This method is used for dynamic modules, when there's often more than one
     * version which can match, but we actually select (or reject) more before selecting.
     *
     * @param unmatchedVersions a collection of unmatched versions
     */
    void unmatched(Collection<String> unmatchedVersions);

    /**
     * Registers the list of rejections that happened during resolution for this module.
     * This method is used for dynamic modules, when there's often more than one
     * version which can match, but we actually select (or reject) more before selecting.
     *
     * @param rejections a collection of rejected versions
     */
    void rejections(Collection<RejectedVersion> rejections);
}