package xapi.server.vertx;

import io.vertx.core.http.HttpServerResponse;
import xapi.fu.ListLike;
import xapi.fu.Out2;
import xapi.scope.impl.AbstractResponse;
import xapi.util.api.Destroyable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/30/17.
 */
public class VertxResponse extends AbstractResponse implements Destroyable {

    private final HttpServerResponse response;

    public VertxResponse(HttpServerResponse response) {
        this.response = response;
        onFinish(s->{
            response.setStatusCode(getStatusCode());
            for (Out2<String, ListLike<String>> header : getHeaders().forEachItem()) {
                String name = header.out1();
                for (String value : header.out2()) {
                    response.putHeader(name, value);
                }
            }
            response.end(getResponseBody());
        });
    }

    @Override
    public void destroy() {
        // cleanup anything leftover here...
    }

    public HttpServerResponse getResponse() {
        return response;
    }
}
