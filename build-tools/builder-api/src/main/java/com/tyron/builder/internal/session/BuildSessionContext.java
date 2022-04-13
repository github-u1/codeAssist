package com.tyron.builder.internal.session;

import com.tyron.builder.api.internal.invocation.BuildAction;
import com.tyron.builder.api.internal.reflect.service.ServiceRegistry;
import com.tyron.builder.internal.buildTree.BuildActionRunner;

public interface BuildSessionContext {
    ServiceRegistry getServices();

    BuildActionRunner.Result execute(BuildAction action);
}