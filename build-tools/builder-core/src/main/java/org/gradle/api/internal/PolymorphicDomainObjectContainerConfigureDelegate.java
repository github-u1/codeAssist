package org.gradle.api.internal;

import groovy.lang.Closure;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.internal.Cast;
import org.gradle.internal.metaobject.ConfigureDelegate;
import org.gradle.internal.metaobject.DynamicInvokeResult;
import org.gradle.util.ConfigureUtil;

public class PolymorphicDomainObjectContainerConfigureDelegate<T> extends ConfigureDelegate {
    private final PolymorphicDomainObjectContainer<T> _container;

    public PolymorphicDomainObjectContainerConfigureDelegate(Closure<?> configureClosure, PolymorphicDomainObjectContainer<T> container) {
        super(configureClosure, container);
        this._container = container;
    }

    @Override
    protected DynamicInvokeResult _configure(String name) {
        return DynamicInvokeResult.found(_container.create(name));
    }

    @Override
    protected DynamicInvokeResult _configure(String name, Object[] params) {
        if (params.length == 1 && params[0] instanceof Closure) {
            return DynamicInvokeResult.found(_container.create(name, (Closure<?>) params[0]));
        } else if (params.length == 1 && params[0] instanceof Class) {
            return DynamicInvokeResult.found(_container.create(name, Cast.<Class<T>>uncheckedCast(params[0])));
        } else if (params.length == 2 && params[0] instanceof Class && params[1] instanceof Closure){
            return DynamicInvokeResult.found(_container.create(name, Cast.uncheckedCast(params[0]), ConfigureUtil.configureUsing((Closure<?>) params[1])));
        }
        return DynamicInvokeResult.notFound();
    }
}
