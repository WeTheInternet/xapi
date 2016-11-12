package xapi.scope.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.fu.MapLike;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public abstract class AbstractScope <Self extends AbstractScope<Self>> implements Scope {

    private boolean released;
    private ClassTo<Object> local;

    protected AbstractScope() {
        local = X_Collect.newClassMap(Object.class);
    }

    protected AbstractScope(Scope parent) {
        this();
        setParent(parent);
    }

    @Override
    public <T, C extends T> MapLike<Class<C>, T> localData() {
        final MapLike/*<Class<?>, Object>*/ localData = local.asMap();
        return localData;
    }

    @Override
    public <T, C extends T> T getLocal(Class<C> cls) {
        return (T) local.get(cls);
    }

    @Override
    public boolean hasLocal(Class cls) {
        return local.containsKey(cls);
    }

    @Override
    public <T, C extends T> T setLocal(Class<C> cls, T value) {
        return (T)local.put(cls, value);
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public final void release() {
        released = true;
        onRelease();
    }

    protected void onRelease() {
    }

}
