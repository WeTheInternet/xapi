package xapi.server.api;

import xapi.fu.In1;
import xapi.scope.request.RequestScope;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestLike;
import xapi.scope.request.ResponseLike;

/**
 * A simple service interface for handling remote requests.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public interface XapiEndpoint<Req extends RequestLike, Resp extends ResponseLike> {

    void serviceRequest(String path, RequestScope<Req, Resp> req, String payload, In1<Req> callback);

    default void initialize(Scope scope, XapiServer<?, ?> server) {

    }

}
