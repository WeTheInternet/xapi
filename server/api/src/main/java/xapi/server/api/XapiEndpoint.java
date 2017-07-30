package xapi.server.api;

import xapi.fu.In1;
import xapi.scope.request.RequestScope;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestLike;

/**
 * A simple service interface for handling remote requests.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public interface XapiEndpoint<Req extends RequestLike> {

    void serviceRequest(String path, RequestScope<Req> req, String payload, In1<Req> callback);

    default void initialize(Scope scope, XapiServer<?, ?> server) {

    }

}
