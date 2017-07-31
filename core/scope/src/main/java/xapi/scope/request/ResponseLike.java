package xapi.scope.request;

import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.ListLike;
import xapi.fu.MapLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/22/17.
 */
public interface ResponseLike {

    int getStatusCode();

    ResponseLike setStatusCode(int code);

    MapLike<String, ListLike<String>> getHeaders();

    HtmlBuffer buildHtmlResponse();

    PrintBuffer buildRawResponse();

    ResponseLike finish();

    default ResponseLike finish(int status) {
        setStatusCode(status);
        return finish();
    }

    ResponseLike onFinish(In1<ResponseLike> callback);

    default ResponseLike onFinish(Do callback) {
        return onFinish(callback.ignores1());
    }


    // TODO Create a ModelBuffer and expose that (with options to emit json?)
    // TODO Create a XapiBuffer, with fluent xml, java, json and css builders

    default ResponseLike addHeader(String name, String value) {
        getHeaders().get(name).add(value);
        return this;
    }



}
