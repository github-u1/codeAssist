package org.gradle.internal.lazy;

/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.function.Supplier;

/**
 * This is basically the same thing as Guava's NonSerializableMemoizingSupplier
 */
@ThreadSafe
class LockingLazy<T> implements Lazy<T> {
    private volatile Supplier<T> supplier;
    private volatile boolean initialized;
    // "value" does not need to be volatile; visibility piggy-backs
    // on volatile read of "initialized".
    @Nullable
    private T value;

    public LockingLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        // A 2-field variant of Double Checked Locking.
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    T t = supplier.get();
                    value = t;
                    initialized = true;
                    // Release the delegate to GC.
                    supplier = null;
                    return t;
                }
            }
        }
        return value;
    }
}