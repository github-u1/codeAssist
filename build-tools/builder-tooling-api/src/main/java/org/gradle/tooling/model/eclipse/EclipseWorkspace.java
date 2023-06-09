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

package org.gradle.tooling.model.eclipse;

import org.gradle.tooling.model.eclipse.EclipseWorkspaceProject;

import java.io.File;
import java.util.List;

/**
 * Information about the eclipse workspace.
 *
 * @since 5.5
 */
public interface EclipseWorkspace {
    /**
     * The filesystem location of the eclipse workspace
     */
    File getLocation();

    /**
     * The list of projects in the eclipse workspace.
     *
     * This list should include all projects and not make a distinction between gradle and non gradle projects.
     */
    List<EclipseWorkspaceProject> getProjects();
}
