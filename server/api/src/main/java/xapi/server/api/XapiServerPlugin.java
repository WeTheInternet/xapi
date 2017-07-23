package xapi.server.api;

import xapi.fu.In1;
import xapi.util.api.RequestLike;

/**
 * A plugin used to install some functionality into a XapiServer.
 *
 * Primarily used to give generated code somewhere to collect up
 * instructions that can be composed into a running server (instead
 * of trying to redefine the entire server, we can leave it up to
 * implementors to decide how to service request, so we can focus only
 * on what to service).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/9/17.
 */
public interface XapiServerPlugin <Request extends RequestLike, Response> {

    /**
     * This is called before a XapiServer server is started
     * (possibly before it is created),
     * to allow you to install Routes into the webapp.
     *
     * Currently, at least in tests, our server is being created from an empty WebApp,
     * and then we install routes and endpoints, then start the server.
     * As such, we need to either ensure all servers tolerate mutation,
     * or we need to enforce cleaner timing semantics.
     *
     * @param app - A {@link WebApp} model for you to mutate
     * @return a callback to peek at the server after it is created,
     * but before it is started.
     *
     * If you need to install endpoints to a server instance,
     * it would look like this:
     * <pre>
     * In1<XapiServer<?,?>> installToServer(WebApp app) {
     *     app.getRoute().add(myRoute("/myPath"));
     *     return server->{
     *        server.registerEndpoint("
     *     }
     * }
     * </pre>
     *
     */
    In1<XapiServer<Request, Response>> installToServer(WebApp app);

}
