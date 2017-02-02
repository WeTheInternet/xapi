package xapi.server.api;

import xapi.collect.api.IntTo;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.scope.api.RequestScope;
import xapi.util.api.RequestLike;

import static xapi.collect.api.IntTo.isEmpty;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public interface XapiServer <Request extends RequestLike, RawRequest> {

    void inScope(RawRequest request, In1Unsafe<RequestScope<Request>> callback);

    void start();

    void shutdown();

    void serviceRequest(RawRequest request, In2<Request, RawRequest> callback);

    String getPath(Request req);

    String getMethod(Request req);

    WebApp getWebApp();

    default String getParam(Request req, String param) {
        final IntTo<String> params = getHeaders(req, param);
        if (isEmpty(params)) {
            return null;
        }
        return params.at(0);
    }

    IntTo<String> getParams(Request req, String param);

    default String getHeader(Request req, String header) {
        final IntTo<String> headers = getHeaders(req, header);
        if (isEmpty(headers)) {
            return null;
        }
        return headers.at(0);
    }

    IntTo<String> getHeaders(Request req, String header);

    void writeText(RequestScope<Request> request, String payload, In1<Request> callback);

    void writeFile(RequestScope<Request> request, String payload, In1<Request> callback);

    void writeGwtJs(RequestScope<Request> request, String payload, In1<Request> callback);

    void writeCallback(RequestScope<Request> request, String payload, In1<Request> callback);
}
