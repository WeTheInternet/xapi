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

    /**
     * Notifies endpoint of failure.
     *
     * You do NOT have to try to handle the error in any way;
     * this is primarily for your cleanup, or possibly error recovery.
     *
     * @param scope The current request
     * @param fail The failure (never null here)
     * @return null to turn failure into success,
     * any other Throwable (i.e. the fail parameter) to fail request.
     *
     * Standard behavior is to simply return the fail parameter.
     *
     */
    default Throwable onFail(Request scope, Throwable fail) {
        return fail;
    }

    default void onSuccess(Request scope) {
    }

    default boolean isContextPath() {
        return true;
    }
}
