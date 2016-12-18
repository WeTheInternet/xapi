package xapi.scope.api;

import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface RequestScope<RequestType extends RequestLike> extends Scope {

    RequestType getRequest();

    void initialize(RequestType req);

    @Override
    default Class<? extends Scope> forScope() {
        return xapi.scope.api.RequestScope.class;
    }
}
