package xapi.util.api;

import xapi.fu.Out1;
import xapi.fu.Out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/26/16.
 */
public interface RequestLike {

    String getPath();

    String getBody();

    default String getParam(String name, Out1<String> dflt) {
        for (Out2<String, Iterable<String>> param : getParams()) {
            if (name.equals(param.out1())) {
                return param.out2().iterator().next();
            }
        }
        return dflt.out1();
    }

    default String getHeader(String name, Out1<String> dflt) {
        for (Out2<String, Iterable<String>> header : getHeaders()) {
            if (name.equals(header.out1())) {
                return header.out2().iterator().next();
            }
        }
        return dflt.out1();
    }

    Iterable<Out2<String, Iterable<String>>> getParams();

    Iterable<Out2<String, Iterable<String>>> getHeaders();

}
