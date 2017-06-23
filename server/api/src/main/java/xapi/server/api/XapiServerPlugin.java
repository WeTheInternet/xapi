package xapi.server.api;

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

    void installToServer(XapiServer<Request, Response> app);

}
