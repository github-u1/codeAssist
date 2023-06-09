package org.gradle.api.internal;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Namer;
import org.gradle.api.internal.collections.CollectionFilter;
import org.gradle.api.internal.collections.SortedSetElementSource;
import org.gradle.internal.Cast;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.Predicates;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import groovy.lang.Closure;

public class DefaultNamedDomainObjectSet<T> extends DefaultNamedDomainObjectCollection<T> implements NamedDomainObjectSet<T> {
    private final MutationGuard parentMutationGuard;

    public DefaultNamedDomainObjectSet(Class<? extends T> type, Instantiator instantiator, Namer<? super T> namer, CollectionCallbackActionDecorator decorator) {
        super(type, new SortedSetElementSource<T>(new Namer.Comparator<T>(namer)), instantiator, namer, decorator);
        this.parentMutationGuard = MutationGuards.identity();
    }

    public DefaultNamedDomainObjectSet(Class<? extends T> type, Instantiator instantiator, CollectionCallbackActionDecorator decorator) {
        this(type, instantiator, Named.Namer.forType(type), decorator);
    }

    // should be protected, but use of the class generator forces it to be public
    public DefaultNamedDomainObjectSet(DefaultNamedDomainObjectSet<? super T> collection, CollectionFilter<T> filter, Instantiator instantiator, Namer<? super T> namer) {
        this(collection, filter, instantiator, namer, MutationGuards.identity());
    }

    public DefaultNamedDomainObjectSet(DefaultNamedDomainObjectSet<? super T> collection, CollectionFilter<T> filter, Instantiator instantiator, Namer<? super T> namer, MutationGuard parentMutationGuard) {
        super(collection, filter, instantiator, namer);
        this.parentMutationGuard = parentMutationGuard;
    }

    @Override
    protected <S extends T> DefaultNamedDomainObjectSet<S> filtered(CollectionFilter<S> filter) {
        return Cast.uncheckedNonnullCast(getInstantiator().newInstance(DefaultNamedDomainObjectSet.class, this, filter, getInstantiator(), getNamer()));
    }

    @Override
    public String getDisplayName() {
        return getTypeDisplayName() + " set";
    }

    @Override
    public <S extends T> NamedDomainObjectSet<S> withType(Class<S> type) {
        return filtered(createFilter(type));
    }

    @Override
    public NamedDomainObjectSet<T> matching(Predicate<? super T> spec) {
        return filtered(createFilter(spec));
    }

    @Override
    public NamedDomainObjectSet<T> matching(Closure spec) {
        return matching(Predicates.<T>convertClosureToSpec(spec));
    }

    @Override
    public Set<T> findAll(Closure cl) {
        return findAll(cl, new LinkedHashSet<T>());
    }

    @Override
    protected <I extends T> Action<? super I> withMutationDisabled(Action<? super I> action) {
        return parentMutationGuard.withMutationDisabled(super.withMutationDisabled(action));
    }
}
