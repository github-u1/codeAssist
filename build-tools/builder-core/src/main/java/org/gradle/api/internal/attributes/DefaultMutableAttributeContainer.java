package org.gradle.api.internal.attributes;

import com.google.common.collect.Sets;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.api.provider.Provider;
import org.gradle.internal.Cast;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class DefaultMutableAttributeContainer implements AttributeContainerInternal {
    private final ImmutableAttributesFactory immutableAttributesFactory;
    private final AttributeContainerInternal parent;
    private ImmutableAttributes state = ImmutableAttributes.EMPTY;
    private Map<Attribute<?>, Provider<?>> lazyAttributes = Cast.uncheckedCast(Collections.EMPTY_MAP);

    public DefaultMutableAttributeContainer(ImmutableAttributesFactory immutableAttributesFactory) {
        this(immutableAttributesFactory, null);
    }

    public DefaultMutableAttributeContainer(ImmutableAttributesFactory immutableAttributesFactory, @Nullable AttributeContainerInternal parent) {
        this.immutableAttributesFactory = immutableAttributesFactory;
        this.parent = parent;
    }

    @Override
    public String toString() {
        return asImmutable().toString();
    }

    @Override
    public Set<Attribute<?>> keySet() {
        if (parent == null) {
            return nonParentKeys();
        } else {
            return Sets.union(parent.keySet(), nonParentKeys());
        }
    }

    private Set<Attribute<?>> nonParentKeys() {
        return Sets.union(state.keySet(), lazyAttributes.keySet());
    }

    @Override
    public <T> AttributeContainer attribute(Attribute<T> key, T value) {
        checkInsertionAllowed(key);
        doInsertion(key, value);
        return this;
    }

    private <T> void doInsertion(Attribute<T> key, T value) {
        assertAttributeValueIsNotNull(value);
        assertAttributeTypeIsValid(value.getClass(), key);
        state = immutableAttributesFactory.concat(state, key, value);
    }

    @Override
    public <T> AttributeContainer attributeProvider(Attribute<T> key, Provider<? extends T> provider) {
        checkInsertionAllowed(key);
        assertAttributeValueIsNotNull(provider);
        // We can only sometimes check the type of the provider ahead of time.
        // When realizing this provider and inserting its value into the container, we still
        // check the value type is appropriate. see doInsertion
        if (provider instanceof ProviderInternal) {
            Class<T> valueType = Cast.<ProviderInternal<T>>uncheckedCast(provider).getType();
            if (valueType != null) {
                assertAttributeTypeIsValid(valueType, key);
            }
        }
        addLazyAttribute(key, provider);
        return this;
    }

    private <T> void checkInsertionAllowed(Attribute<T> key) {
        // Don't just use keySet() method instead, since we should be allowed to override attributes already in the parent
        for (Attribute<?> attribute : nonParentKeys()) {
            String name = key.getName();
            if (attribute.getName().equals(name) && attribute.getType() != key.getType()) {
                throw new IllegalArgumentException("Cannot have two attributes with the same name but different types. "
                    + "This container already has an attribute named '" + name + "' of type '" + attribute.getType().getName()
                    + "' and you are trying to store another one of type '" + key.getType().getName() + "'");
            }
        }
    }

    /**
     * Checks that the attribute's type matches the given value's type is the expected value type.
     *
     * @param valueType the value type to check
     * @param attribute the attribute containing a type to check against
     */
    private <T> void assertAttributeTypeIsValid(Class<?> valueType, Attribute<T> attribute) {
        if (!attribute.getType().isAssignableFrom(valueType)) {
            throw new IllegalArgumentException(String.format("Unexpected type for attribute '%s' provided. Expected a value of type %s but found a value of type %s.", attribute.getName(), attribute.getType().getName(), valueType.getName()));
        }
    }

    private void assertAttributeValueIsNotNull(@Nullable Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting null as an attribute value is not allowed");
        }
    }

    @Override
    public <T> T getAttribute(Attribute<T> key) {
        T attribute = state.getAttribute(key);
        if (attribute == null && lazyAttributes.containsKey(key)) {
            attribute = realizeLazyAttribute(key);
        }
        if (attribute == null && parent != null) {
            attribute = parent.getAttribute(key);
        }
        return attribute;
    }

    @Override
    public boolean isEmpty() {
        return keySet().isEmpty();
    }

    @Override
    public boolean contains(Attribute<?> key) {
        return keySet().contains(key);
    }

    @Override
    public ImmutableAttributes asImmutable() {
        realizeAllLazyAttributes();

        if (parent == null) {
            return state;
        } else {
            ImmutableAttributes attributes = parent.asImmutable();
            if (!state.isEmpty()) {
                attributes = immutableAttributesFactory.concat(attributes, state);
            }
            return attributes;
        }
    }

    @Override
    public Map<Attribute<?>, ?> asMap() {
        return asImmutable().asMap();
    }

    @Override
    public AttributeContainer getAttributes() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultMutableAttributeContainer that = (DefaultMutableAttributeContainer) o;

        if (!Objects.equals(parent, that.parent)) {
            return false;
        }
        if (!Objects.equals(asImmutable(), that.asImmutable())) {
            return false;
        }

        return state.equals(that.state);
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + state.hashCode();
        result = 31 * result + asImmutable().hashCode();
        return result;
    }

    private <T> void addLazyAttribute(Attribute<T> key, Provider<? extends T> provider) {
        if (lazyAttributes == Collections.EMPTY_MAP) {
            lazyAttributes = new LinkedHashMap<>(1);
        }
        lazyAttributes.put(key, provider);
    }

    private <T> T realizeLazyAttribute(Attribute<T> key) {
        Provider<? extends T> provider = removeLazyAttribute(key);
        final T value = provider.get();
        attribute(key, value);
        return value;
    }

    private void realizeAllLazyAttributes() {
        if (!lazyAttributes.isEmpty()) {
            lazyAttributes.forEach((key, value) -> doInsertion(Cast.uncheckedNonnullCast(key), (Object) value.get()));
            lazyAttributes.clear();
        }
    }

    private <T> Provider<? extends T> removeLazyAttribute(Attribute<T> key) {
        // This can only be called once we know the key is in the lazyAttributes map
        return Cast.uncheckedNonnullCast(lazyAttributes.remove(key));
    }
}
