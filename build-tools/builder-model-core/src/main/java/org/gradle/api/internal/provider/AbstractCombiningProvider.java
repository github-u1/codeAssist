package org.gradle.api.internal.provider;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractCombiningProvider<OUT, BASE, IN> extends AbstractMinimalProvider<OUT> {
    private final Class<OUT> type;
    private final ProviderInternal<? extends BASE> left;
    private final ProviderInternal<? extends IN> right;

    public AbstractCombiningProvider(Class<OUT> type, ProviderInternal<? extends BASE> left, ProviderInternal<? extends IN> right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Nullable
    @Override
    public Class<OUT> getType() {
        return type;
    }

    @Override
    public boolean calculatePresence(ValueConsumer consumer) {
        return left.calculatePresence(consumer) && right.calculatePresence(consumer);
    }

    @Override
    protected Value<OUT> calculateOwnValue(ValueConsumer consumer) {
        Value<? extends BASE> leftValue = left.calculateValue(consumer);
        if (leftValue.isMissing()) {
            return leftValue.asType();
        }
        Value<? extends IN> rightValue = right.calculateValue(consumer);
        if (rightValue.isMissing()) {
            return rightValue.asType();
        }
        return Value.of(map(leftValue.get(), rightValue.get()));
    }

    protected abstract OUT map(BASE b, IN v);

    @Override
    public String toString() {
        return String.format("combine(%s, %s)", left, right);
    }
}
