package org.gradle.api.internal.provider;

import org.gradle.api.provider.Provider;

/**
 * A {@link Provider} that always has a value defined. The value may not necessarily be final.
 */
public abstract class AbstractProviderWithValue<T> extends AbstractMinimalProvider<T> {
    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public boolean calculatePresence(ValueConsumer consumer) {
        return true;
    }

    @Override
    public Provider<T> orElse(T value) {
        return this;
    }

    @Override
    public Provider<T> orElse(Provider<? extends T> provider) {
        return this;
    }
}