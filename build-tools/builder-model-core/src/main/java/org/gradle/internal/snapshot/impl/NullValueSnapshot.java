package org.gradle.internal.snapshot.impl;

import com.google.common.hash.Hasher;
import org.gradle.internal.isolation.Isolatable;
import org.gradle.internal.snapshot.ValueSnapshot;
import org.gradle.internal.snapshot.ValueSnapshotter;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class NullValueSnapshot implements ValueSnapshot, Isolatable<Object> {
    public static final NullValueSnapshot INSTANCE = new NullValueSnapshot();

    private NullValueSnapshot() {
    }

    @Override
    public ValueSnapshot snapshot(@Nullable Object value, ValueSnapshotter snapshotter) {
        if (value == null) {
            return this;
        }
        return snapshotter.snapshot(value);
    }

    @Override
    public void appendToHasher(Hasher hasher) {
        hasher.putString("null", StandardCharsets.UTF_8);
    }

    @Override
    public ValueSnapshot asSnapshot() {
        return this;
    }

    @Override
    public Object isolate() {
        return null;
    }

    @Nullable
    @Override
    public <S> S coerce(Class<S> type) {
        return null;
    }
}