package org.gradle.api.internal.tasks.properties.annotations;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.internal.provider.PropertyInternal;
import org.gradle.internal.instantiation.PropertyRoleAnnotationHandler;
import org.gradle.internal.state.ModelObject;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class OutputPropertyRoleAnnotationHandler implements PropertyRoleAnnotationHandler {
    private final ImmutableSet<Class<? extends Annotation>> annotations;

    public OutputPropertyRoleAnnotationHandler(List<AbstractOutputPropertyAnnotationHandler> handlers) {
        ImmutableSet.Builder<Class<? extends Annotation>> builder = ImmutableSet.builderWithExpectedSize(handlers.size());
        for (AbstractOutputPropertyAnnotationHandler handler : handlers) {
            builder.add(handler.getAnnotationType());
        }
        this.annotations = builder.build();
    }

    @Override
    public Set<Class<? extends Annotation>> getAnnotationTypes() {
        return annotations;
    }

    @Override
    public void applyRoleTo(ModelObject owner, Object target) {
        if (target instanceof PropertyInternal) {
            ((PropertyInternal<?>) target).attachProducer(owner);
        }
    }
}
