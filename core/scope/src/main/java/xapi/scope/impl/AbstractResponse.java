package xapi.scope.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.*;
import xapi.log.X_Log;
import xapi.scope.spi.ResponseLike;
import xapi.util.X_Debug;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/30/17.
 */
public class AbstractResponse implements ResponseLike {

    private int statusCode = -1;
    private MapLike<String, ListLike<String>> headers;
    private HtmlBuffer html;
    private PrintBuffer raw;
    private boolean closed;
    private Mutable<In1<ResponseLike>> onFinish = new Mutable<>(In1.ignored());

    @Override
    public int getStatusCode() {
        return statusCode == -1 ? 200 : statusCode;
    }

    @Override
    public ResponseLike setStatusCode(int code) {
        if (this.statusCode != -1 && this.statusCode != code) {
            X_Log.info(AbstractResponse.class, "Changing status code from ", this.statusCode, " to ", code);
        }
        this.statusCode = code;
        return this;
    }

    @Override
    public MapLike<String, ListLike<String>> getHeaders() {
        if (headers == null) {
            headers = initHeaders();
        }
        return headers;
    }

    protected MapLike<String,ListLike<String>> initHeaders() {
        return X_Collect.newStringMultiMap(String.class)
            .mapValue(In2.<String, ListLike<String>>in2Unsafe((k, v)->{
            throw new UnsupportedOperationException("Multimap does not support bring-your-own-list");
        }).supply1(null), IntTo::asListLike);
    }

    @Override
    public HtmlBuffer buildHtmlResponse() {
        if (raw != null) {
            throw new IllegalStateException("Cannot use HtmlBuffer after raw PrintBuffer is in use");
        }
        if (html == null) {
            html = initHtmlBuffer();
        }
        return html;
    }

    protected HtmlBuffer initHtmlBuffer() {
        return new HtmlBuffer();
    }

    public PrintBuffer errorBuffer() {
        if (raw != null) {
            // TODO: add a DomBuffer (w/ error styling) to raw and return that instead.
            return raw;
        }
        if (html == null) {
            html = initHtmlBuffer();
        }
        // TODO: add some error wrapping here (a tag inside the body)
        return html.getBody();
    }

    @Override
    public PrintBuffer buildRawResponse() {
        if (html != null) {
            throw new IllegalStateException("Cannot use raw PrintBuffer after HtmlBuffer is in use");
        }
        if (raw == null) {
            raw = initRawBuffer();
        }
        return raw;
    }

    protected PrintBuffer initRawBuffer() {
        return new PrintBuffer();
    }

    @Override
    public ResponseLike finish() {
        if (closed) {
            X_Debug.preventInterleave(()->
                X_Log.warn(AbstractResponse.class, "Response already closed")
            );
        } else {
            flushFinished();
            closed = true;
        }
        return this;
    }

    private void flushFinished() {
        beforeFinished();
        onFinish.out1().in(this);
        afterFinished();
    }

    protected void afterFinished() {
    }

    protected void beforeFinished() {

    }

    @Override
    public ResponseLike onFinish(In1<ResponseLike> callback) {
        onFinish.process(In1::useAfterMe, callback.onlyOnce());
        return this;
    }

    protected String getResponseBody() {
        if (raw != null) {
            return raw.toSource();
        }
        if (html != null) {
            return html.toString();
        }
        return "";
    }

    @Override
    public String clearResponseBody() {
        String body = getResponseBody();
        raw = null;
        html = null;
        return body;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean hasBody() {
        if (raw != null) {
            if (raw.isNotEmpty()) {
                return true;
            }
        }
        if (html != null) {
            if (html.hasBody()) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        closed = false;
    }

    @Override
    public void reroute(String newRoute, boolean updateUrl) {
        if (!updateUrl) {
            X_Log.warn(AbstractResponse.class, getClass(), " should override reroute to handle updateUrl=false reroutes");
        }
        clearResponseBody();
        setStatusCode(307);
        setHeader("Location", newRoute);
    }
}
