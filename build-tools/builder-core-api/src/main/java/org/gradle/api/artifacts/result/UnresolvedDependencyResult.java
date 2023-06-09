package org.gradle.api.artifacts.result;

import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.internal.scan.UsedByScanPlugin;

/**
 * A dependency that could not be resolved.
 */
@UsedByScanPlugin
public interface UnresolvedDependencyResult extends DependencyResult {
    /**
     * Returns the selector that was attempted to be resolved. This may not be the same as the requested component.
     */
    ComponentSelector getAttempted();

    /**
     * Returns the reasons why the failed selector was attempted.
     */
    ComponentSelectionReason getAttemptedReason();

    /**
     * The failure that occurred.
     */
    Throwable getFailure();
}
