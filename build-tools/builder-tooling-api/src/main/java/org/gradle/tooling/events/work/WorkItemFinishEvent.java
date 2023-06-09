/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.tooling.events.work;

import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.work.WorkItemOperationResult;
import org.gradle.tooling.events.work.WorkItemProgressEvent;

/**
 * An event that informs about a work item having finished its execution.
 *
 * @since 5.1
 */
public interface WorkItemFinishEvent extends WorkItemProgressEvent, FinishEvent {
    @Override
    WorkItemOperationResult getResult();
}
