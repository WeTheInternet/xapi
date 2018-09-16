package xapi.server.vertx;

import io.vertx.core.http.HttpServerResponse;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.log.X_Log;
import xapi.scope.impl.AbstractResponse;
import xapi.scope.spi.ResponseLike;
import xapi.util.X_String;
import xapi.util.api.Destroyable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/30/17.
 */
public class VertxResponse extends AbstractResponse implements Destroyable {

    private HttpServerResponse response;
    private boolean calledPrepareToClose;
    private In2<String, Boolean> rerouter;

    public VertxResponse(HttpServerResponse response) {
        this.response = response;
        rerouter = super::reroute;
    }

    @Override
    public String prepareToClose() {
        calledPrepareToClose = true;
        response.setStatusCode(getStatusCode());
        final MapLike<String, ListLike<String>> headers = getHeaders();
        final String body = clearResponseBody();
        final boolean writeBody = !X_String.isEmpty(body);
        if (writeBody) {
            // TODO: use / check for chunked...
            final ListLike<String> length = headers.get("Content-Length");
            if (length.isEmpty()) {
                length.add(Integer.toString(body.length()));
            } else {
                int was = Integer.parseInt(length.get(0));
                length.set(0, Integer.toString(was + body.length()));
            }

        }
        for (Out2<String, ListLike<String>> header : headers.forEachItem()) {
            String name = header.out1();
            for (String value : header.out2()) {
                response.putHeader(name, value);
            }
        }
        return body;
    }

    @Override
    protected void afterFinished() {
        if (isClosed()) {
            if (!calledPrepareToClose) {
                X_Log.trace(VertxResponse.class, "Already closed when finished...", "call Response.prepareToClose() to suppress this error");
            } else {
                X_Log.trace(VertxResponse.class, "Already closed when finished...");
            }
            return;
        }
        if (calledPrepareToClose) {
            return;
        }
        final String body = prepareToClose();
        if (!X_String.isEmpty(body)) {
            response.write(body);
        }
    }

    @Override
    public boolean isClosed() {
        final boolean weAreClosed = super.isClosed();
        if (response.ended() != weAreClosed) {
            if (calledPrepareToClose && response.ended()) {
                // user was being good.  They let us know they finished the response
            } else {
                // user is being bad.  They are accidentally trying to end the vertx response directly instead of going through us
                X_Log.error(VertxResponse.class, "Vertx Response was manually ended without closing xapi Response (call .prepareToClose())");
            }
        }
        return weAreClosed;
    }

    @Override
    public final void close() {
        finish();
        destroy();
    }

    @Override
    public ResponseLike finish() {
        super.finish();
        if (!response.ended()) {
            response.setStatusCode(getStatusCode());
            if (!calledPrepareToClose) {
                String body = prepareToClose();
                response.end(body);
            } else {
                response.end();
            }
        }
        return this;
    }

    @Override
    public void destroy() {
        // cleanup anything leftover here...
        response = null;
    }

    public HttpServerResponse getHttpResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "VertxResponse{" +
            "response=" + response +
            ", calledPrepareToClose=" + calledPrepareToClose +
            "} " + super.toString();
    }

    public void setHttpResponse(HttpServerResponse httpResponse) {
        this.response = httpResponse;
        reset();
    }

    public In2<String, Boolean> getRerouter() {
        return rerouter;
    }

    public void setRerouter(In2<String, Boolean> rerouter) {
        this.rerouter = rerouter == null ? super::reroute : rerouter;
    }

    @Override
    public void reroute(String newRoute, boolean updateUrl) {
        rerouter.in(newRoute, updateUrl);
    }

    @Override
    public void reset() {
        calledPrepareToClose = false;
        super.reset();
    }
}
