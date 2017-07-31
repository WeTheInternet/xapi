package xapi.scope.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.ListLike;
import xapi.fu.MapLike;
import xapi.fu.Mutable;
import xapi.log.X_Log;
import xapi.scope.request.ResponseLike;

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
            X_Log.warn(AbstractResponse.class, "Response already closed");
        } else {
            onFinish.out1().in(this);
            closed = true;
        }
        return this;
    }

    @Override
    public ResponseLike onFinish(In1<ResponseLike> callback) {
        onFinish.process(In1::useAfterMe, callback);
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
}
