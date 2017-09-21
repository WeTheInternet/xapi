package xapi.scope;

import xapi.fu.Do;
import xapi.inject.X_Inject;
import xapi.scope.api.Scope;
import xapi.scope.service.ScopeService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public class X_Scope {

    private static final ScopeService service = X_Inject.singleton(ScopeService.class);

    public static Do inheritScope() {
        return service.inheritScope();
    }

    public static ScopeService service() {
        return service;
    }

    public static Scope currentScope() {
        return service().currentScope();
    }
    public static Scope currentScopeIfNull(Scope maybeNull) {
        return maybeNull == null ? currentScope() : maybeNull;
    }
}
