package xapi.scope.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.MapLike;
import xapi.fu.Mutable;
import xapi.fu.ReturnSelf;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public abstract class AbstractScope <Self extends AbstractScope<Self>> implements Scope, ReturnSelf<Self> {

    private boolean released;
    private ClassTo<Object> local;
    private Mutable<Do> cleanup = new Mutable<>(Do.NOTHING);

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
        assert !released : "Do not add values to a released scope; instead, implement Immortal and call reincarnateIfNeeded()";
        return (T)local.put(cls, value);
    }

    public Self reincarnateIfNeeded() {
        return released ? reincarnate() : self();
    }
    /**
     * This method exists
     * @return
     */
    protected Self reincarnate() {
        throw new NotYetImplemented(getClass(), "Must implement reincarnate");
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public final void release() {
        final boolean shouldRun = !released;
        released = true;
        if (shouldRun) {
            onRelease();
        }
        // once released,
        local.clear();
    }

    protected void onRelease() {
        this.cleanup.out1().done();
    }

    @Override
    public void onDetached(Do cleanup) {
        this.cleanup.process(Do::doAfter, cleanup);
    }
}
