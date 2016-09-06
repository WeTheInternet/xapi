package xapi.scope.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public abstract class AbstractScope <Self extends AbstractScope<Self>> implements Scope {

    private boolean released;
    private ClassTo<Object> local;
    private Scope parent;

    protected AbstractScope() {
        local = X_Collect.newClassMap(Object.class);
    }

    protected AbstractScope(Scope parent) {
        this();
        this.parent = parent;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    @Override
    public <T, C extends T> T getLocal(Class<C> cls) {
        return (T) local.get(cls.getName());
    }

    @Override
    public boolean hasLocal(Class<?> cls) {
        return local.containsKey(cls);
    }

    @Override
    public <T, C extends T> T set(Class<C> cls, T value) {
        return (T)local.put(cls, value);
    }

    @Override
    public final void release() {
        released = true;
        onRelease();
    }

    protected void onRelease() {
    }

    @Override
    public Scope getParent() {
        return parent;
    }

}
