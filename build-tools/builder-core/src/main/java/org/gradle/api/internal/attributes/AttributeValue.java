package org.gradle.api.internal.attributes;

import org.gradle.api.attributes.Attribute;

import javax.annotation.Nullable;

/**
 * Represents an optional attribute value, as found in an attribute container. There are 3 possible cases:
 * <ul>
 *     <li><i>present</i> is the default, and represents an attribute with an actual value</li>
 *     <li><i>missing</i> used whenever an attribute has no value.</li>
 * </ul>
 * During attribute matching, this can be used to implement various {@link org.gradle.api.attributes.AttributeMatchingStrategy strategies}.
 * @param <T> the type of the attribute
 *
 * @since 3.3
 */
public interface AttributeValue<T> {
    AttributeValue<Object> MISSING = new AttributeValue<Object>() {
        @Override
        public boolean isPresent() {
            return false;
        }

        @Nullable
        @Override
        public <S> S coerce(Attribute<S> type) {
            throw new UnsupportedOperationException("coerce() should not be called on a missing attribute value");
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException("get() should not be called on a missing attribute value");
        }
    };

    /**
     * Tells if this attribute value is present.
     * @return true if this attribute value is present, implying not <code>null</code>.
     */
    boolean isPresent();

    /**
     * Returns the value of this attribute.
     * @return the value of this attribute. Throws an error if called on a missing or unknown attribute value.
     */
    T get();

    /**
     * Coerces this value to the type of the other attribute, so it can be compared
     * to a value of that other attribute.
     *
     * @throws IllegalArgumentException if this attribute is not compatible with the other one
     */
    <S> S coerce(Attribute<S> otherAttribute);
}
