package xapi.scope.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.process.Multiplexed;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Lazy;
import xapi.inject.X_Inject;
import xapi.scope.X_Scope;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.Scope;
import xapi.scope.service.ScopeService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
@SingletonDefault(implFor = ScopeService.class)
public class ScopeServiceDefault implements ScopeService {

    protected class ScopeMap {
        protected ClassTo<Scope> scopes = X_Collect.newClassMap(Scope.class);
        protected ThreadLocal<ClassTo<Scope>> multiplexed = X_Scope.localDeferred(()->X_Collect.newClassMap(Scope.class));
        protected ClassTo<In1Out1<Class<? extends Scope>, Scope>> factories = X_Collect.newClassMap(In1Out1.class);
        protected AtomicReference<Scope> scope = new AtomicReference<>();

        protected void inherit(ScopeMap map, Do restoreScope) {
            scopes.putEntries(map.scopes);
            factories.putEntries(map.factories);
            scope.set(map.scope.get());
            multiplexed.get().putEntries(map.multiplexed.get());
            restoreScope.done();
        }

        public Do captureScope() {
            Scope current = scope.get();
            Do restore = Do.NOTHING;
            while (current != null) {
                restore = restore.doBefore(current.captureScope());
                current = current.getParent();
            }
            return restore;
        }
    }

    private ThreadLocal<ScopeMap> currentScope;
    private Lazy<GlobalScope<?>> globalScope = Lazy.deferred1(this::newGlobalScope);

    public ScopeServiceDefault() {
        currentScope = new ThreadLocal<ScopeMap>() {
            @Override
            protected ScopeMap initialValue() {
                return newScopeMap();
            }
        };
    }

    protected ScopeMap newScopeMap() {
        return new ScopeMap();
    }

    protected <S extends Scope, Generic extends S> S createScope(Class<Generic> cls) {
        if (GlobalScope.class.isAssignableFrom(cls)) {
            return (S) globalScope.out1();
        }
        final ScopeMap map = currentScope.get();
        Scope scope;
        // Check key type hierarchy to find .forScope() methods that we can run without an instance

        final boolean multiplexed = Scope.isMultiplexed(cls);
        final ClassTo<Scope> scopeMap = multiplexed ? map.multiplexed.get() : map.scopes;
        scope = scopeMap.get(cls);
        if (scope == null) {
            final In1Out1<Class<? extends Scope>, Scope> factory = map.factories.get(cls);
            if (factory == null) {
                scope = defaultCreate(cls);
            } else {
                scope = factory.io(cls);
            }
            if (scope.getParent() == null) {
                scope.setParent(globalScope.out1());
            }
            scopeMap.put(cls, scope);
            if (multiplexed) {
                scope.onDetached(scopeMap::remove, cls);
            }
        }
        return (S) scope;
    }

    protected <S extends Scope, Generic extends S> S defaultCreate(Class<Generic> cls) {
        return X_Inject.instance(cls);
    }

    @Override
    public Do inheritScope() {
        final ScopeMap map = currentScope.get();
        // we need to close over any threadlocals that need to persist...
        Do restoreScope = map.captureScope();
        return ()->currentScope.get().inherit(map, restoreScope);
    }

    protected GlobalScope<?> newGlobalScope() {
        final GlobalScope<?> scope = X_Inject.instance(GlobalScope.class);
        scope.isAttached();
        return scope;
    }

    @Override
    public <S extends Scope> void runInScope(S scope, In2Unsafe<S, Do> todo) {
        final ScopeMap map = currentScope.get();
        final Scope curScope = map.scope.get();
        boolean multiplexed = scope.getClass().getAnnotation(Multiplexed.class) != null;
        final ClassTo<Scope> scopeMap = multiplexed ? map.multiplexed.get() : map.scopes;
        final Scope forType = scopeMap.get(scope.forScope());
        map.scope.set(scope);
        Do retainer = scope.retain();
        submitTask(todo, scope, ()->{
            map.scope.set(curScope);
            if (forType == null) {
                scopeMap.remove(scope.forScope());
            } else {
                scopeMap.put(scope.forScope(), forType);
            }
            retainer.done();
            maybeRelease(scope);
        });
    }

    protected <S extends Scope> void maybeRelease(S scope) {
        if (!scope.hasRetainers() && scope.forScope() != GlobalScope.class) {
            // TODO: allow for registered retainers to prevent scope from being released...
            scope.release();
        }
    }

    private <S extends Scope> void submitTask(In2Unsafe<S, Do> todo, S scope, Do onDone) {
        todo.in(scope, onDone);
    }

    @Override
    public <S extends Scope, Generic extends S> void runInNewScope(
        Class<Generic> scope, In2Unsafe<Generic, Do> todo
    ) {
        final Generic created = createScope(scope);
        runInScope(created, todo);
    }

    @Override
    public Scope currentScope() {
        return currentScope.get().scope.updateAndGet(s->s==null?globalScope.out1():s);
    }

}
