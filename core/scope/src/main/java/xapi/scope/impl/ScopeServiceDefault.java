package xapi.scope.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Lazy;
import xapi.inject.X_Inject;
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
        protected ClassTo<In1Out1<Class<? extends Scope>, Scope>> factories = X_Collect.newClassMap(In1Out1.class);
        protected AtomicReference<Scope> scope = new AtomicReference<>();

        protected void inherit(ScopeMap map) {
            scopes.putAll(map.scopes);
            factories.putAll(map.factories);
            scope.set(map.scope.get());
        }
    }

    private ThreadLocal<ScopeMap> currentScope;
    private Lazy<GlobalScope> globalScope = Lazy.deferred1(this::newGlobalScope);

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
        if (GlobalScope.class.equals(cls)) {
            return (S) globalScope.out1();
        }
        final ScopeMap map = currentScope.get();
        // Check key type hierarchy to find .forScope() methods that we can run without an instance

        Scope scope = map.scopes.get(cls);
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
            map.scopes.put(cls, scope);
        }
        return (S) scope;
    }

    protected <S extends Scope, Generic extends S> S defaultCreate(Class<Generic> cls) {
        return X_Inject.instance(cls);
    }

    @Override
    public Do inheritScope() {
        final ScopeMap map = currentScope.get();
        return ()->currentScope.get().inherit(map);
    }

    protected GlobalScope newGlobalScope() {
        final GlobalScope scope = X_Inject.instance(GlobalScope.class);
        scope.isAttached();
        return scope;
    }

    @Override
    public <S extends Scope> void runInScope(S scope, In2Unsafe<S, Do> todo) {
        final ScopeMap map = currentScope.get();
        final Scope curScope = map.scope.get();
        final Scope forType = map.scopes.get(scope.forScope());
        map.scope.set(scope);
        submitTask(todo, scope, ()->{
            map.scope.set(curScope);
            map.scopes.put(scope.forScope(), forType);
            scope.release();
        });
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
