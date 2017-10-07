package xapi.dev.api;

import xapi.dev.api.dynamic.Handler;
import xapi.dev.source.NameGen;
import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.fu.Out1;

/**
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class DynamicUrl {

    private final String id;

    public DynamicUrl(NameGen id) {
        this(id.newName("d/"));
    }

    public DynamicUrl(String id) {
        this.id = id;
    }

    public In1Out1<String, String> withValue(String value) {
        return withValue(Immutable.immutable1(value));
    }

    public In1Out1<String, String> withValue(Out1<String> value) {
        return withValue(value.ignoreIn1());
    }

    public In1Out1<String, String> withValue(In1Out1<String, String> value) {
        return Handler.registerDynamicUrl(id, value);
    }

    public void clear() {
        Handler.clearDynamicUrl(id);
    }

    public String getUrl() {
        return "dynamic:" + id;
    }
}
