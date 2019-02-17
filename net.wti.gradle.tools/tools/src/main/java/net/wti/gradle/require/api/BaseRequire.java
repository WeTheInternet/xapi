package net.wti.gradle.require.api;

import groovy.lang.Closure;
import net.wti.gradle.internal.api.ProjectView;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.internal.metaobject.*;
import org.gradle.util.ConfigureUtil;

import java.util.Map;

/**
 * We want the require dsl to behave like a named domain object container,
 * but we don't want to expose the full collection to the user.
 *
 * So, we implement PropertyMixIn and MethodMixIn, with _slightly_ different semantics;
 * we will always create the requested object, and will rely on the XapiSchema
 * to fail if the user has attempted to configure a platform/module that is not expressly supported.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/11/19 @ 6:15 AM.
 */
public abstract class BaseRequire<T> implements PropertyMixIn, MethodMixIn {

    private ContainerElementsDynamicObject dynamic = new ContainerElementsDynamicObject();

    protected abstract ProjectView getView();

    @Override
    public PropertyAccess getAdditionalProperties() {
        return dynamic;
    }

    @Override
    public MethodAccess getAdditionalMethods() {
        return dynamic;
    }

    protected String getDisplayName() {
        return getClass().getSimpleName();
    }

    protected abstract NamedDomainObjectContainer<T> container();

    public T getProperty(String name) {
        return container().maybeCreate(name);
    }

    private class ContainerElementsDynamicObject extends AbstractDynamicObject {
        @Override
        public String getDisplayName() {
            return BaseRequire.this.getDisplayName();
        }

        @Override
        public boolean hasProperty(String name) {
            return container().findByName(name) != null;
        }

        @Override
        public DynamicInvokeResult tryGetProperty(String name) {
            final T result = container().maybeCreate(name);
            return DynamicInvokeResult.found(result);
        }

        @Override
        public Map<String, T> getProperties() {
            return container().getAsMap();
        }

        @Override
        public boolean hasMethod(String name, Object... arguments) {
            return isConfigureMethod(name, arguments);
        }

        @Override
        public DynamicInvokeResult tryInvokeMethod(String name, Object... arguments) {
            if (isConfigureMethod(name, arguments)) {
                return DynamicInvokeResult.found(ConfigureUtil.configure((Closure) arguments[0], container().maybeCreate(name)));
            }
            return DynamicInvokeResult.notFound();
        }

        private boolean isConfigureMethod(String name, Object... arguments) {
            return (arguments.length == 1 && arguments[0] instanceof Closure);
        }
    }
}
