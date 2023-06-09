package org.gradle.model.internal.core;

import javax.annotation.concurrent.NotThreadSafe;
import org.gradle.api.Action;
import org.gradle.model.ModelViewClosedException;
import org.gradle.model.ReadOnlyModelViewException;
import org.gradle.model.WriteOnlyModelViewException;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;

@NotThreadSafe
public class DefaultModelViewState implements ModelViewState {

    private final ModelPath path;
    private final ModelType<?> type;
    private final ModelRuleDescriptor ruleDescriptor;
    private boolean closed;
    private final boolean mutable;
    private final boolean canReadChildren;

    public DefaultModelViewState(ModelPath path, ModelType<?> type, ModelRuleDescriptor ruleDescriptor, boolean mutable, boolean canReadChildren) {
        this.path = path;
        this.type = type;
        this.ruleDescriptor = ruleDescriptor;
        this.mutable = mutable;
        this.canReadChildren = canReadChildren;
    }

    public void close() {
        closed = true;
    }

    public Action<Object> closer() {
        return new Action<Object>() {
            @Override
            public void execute(Object o) {
                close();
            }
        };
    }

    @Override
    public void assertCanMutate() {
        if (closed) {
            throw new ModelViewClosedException(path, type, ruleDescriptor);
        }
        if (!mutable) {
            throw new ReadOnlyModelViewException(path, type, ruleDescriptor);
        }
    }

    @Override
    public void assertCanReadChildren() {
        if (!canReadChildren) {
            throw new WriteOnlyModelViewException(null, path, type, ruleDescriptor);
        }
    }

    @Override
    public void assertCanReadChild(String name) {
        if (!canReadChildren) {
            throw new WriteOnlyModelViewException(name, path, type, ruleDescriptor);
        }
    }

    @Override
    public boolean isCanMutate() {
        return mutable && !closed;
    }

    public boolean isClosed() {
        return closed;
    }
}
