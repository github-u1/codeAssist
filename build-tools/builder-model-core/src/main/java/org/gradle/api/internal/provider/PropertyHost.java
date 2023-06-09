package org.gradle.api.internal.provider;


import org.gradle.internal.state.ModelObject;

import org.jetbrains.annotations.Nullable;

public interface PropertyHost {
    PropertyHost NO_OP = producer -> null;

    /**
     * Returns null if the host allows reads of its state, or a string that explains why reads are not allowed.
     */
    @Nullable
    String beforeRead(@Nullable ModelObject producer);
}