package org.gradle.api.internal;

import org.gradle.api.internal.plugins.ApplyPluginBuildOperationType;
import org.gradle.configuration.ApplyScriptPluginBuildOperationType;
import org.gradle.internal.operations.BuildOperationType;

/**
 * Fired when a domain object collection executes a registered callback that was registered by user code.
 *
 * @since 5.1
 */
public final class ExecuteDomainObjectCollectionCallbackBuildOperationType implements BuildOperationType<ExecuteDomainObjectCollectionCallbackBuildOperationType.Details, ExecuteDomainObjectCollectionCallbackBuildOperationType.Result> {

    public interface Details {

        /**
         * The application ID of the script or plugin that registered the listener.
         *
         * @see ApplyPluginBuildOperationType.Details#getApplicationId()
         * @see ApplyScriptPluginBuildOperationType.Details#getApplicationId()
         */
        long getApplicationId();

    }

    public interface Result {
    }

    static final ExecuteDomainObjectCollectionCallbackBuildOperationType.Result RESULT = new ExecuteDomainObjectCollectionCallbackBuildOperationType.Result() {
    };

    private ExecuteDomainObjectCollectionCallbackBuildOperationType() {
    }
}

