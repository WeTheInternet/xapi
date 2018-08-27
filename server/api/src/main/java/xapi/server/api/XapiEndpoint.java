package xapi.server.api;

import xapi.fu.In2;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestScope;

/**
 * A simple service interface for handling remote requests.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public interface XapiEndpoint<Request extends RequestScope> {

    void serviceRequest(String path, Request req, String payload, In2<Request, Throwable> callback);

    default void initialize(Scope scope, XapiServer<Request> server) {

    }

    default void onFail(Request finalScope, Throwable fail) {

    }

    default void onSuccess(Request finalScope) {

    }

    default boolean isContextPath() {
        return true;
    }
}
