package xapi.scope.request;

import xapi.fu.ListLike;
import xapi.fu.MapLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/22/17.
 */
public interface ResponseLike {

    int getStatusCode();

    ResponseLike setStatusCode(int code);

    MapLike<String, ListLike<String>> getHeaders();

    default ResponseLike addHeader(String name, String value) {
        getHeaders().get(name).add(value);
        return this;
    }



}
