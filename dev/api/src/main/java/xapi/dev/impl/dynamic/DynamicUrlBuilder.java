package xapi.dev.impl.dynamic;

import xapi.dev.api.DynamicUrl;
import xapi.dev.source.NameGen;
import xapi.fu.Do;
import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/17.
 */
public class DynamicUrlBuilder {

    private String path;
    private NameGen nameGen;

    public In1Out1<String, String> withValue(String value) {
        return withValue(Immutable.immutable1(value));
    }

    public In1Out1<String, String> withValue(Out1<String> value) {
        return withValue(value.ignoreIn1());
    }

    public In1Out1<String, String> withValue(In1Out1<String, String> value) {
        return Handler.registerDynamicUrl(path, value);
    }

    public void clear() {
        Handler.clearDynamicUrl(path);
    }

    public DynamicUrlBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public DynamicUrl build() {
        DynamicUrl url;
        final Do clearHandler = Do.of(Handler::clearDynamicUrl, path);
        if (path == null) {
            url = new DynamicUrl(nameGen, clearHandler);
        } else {
            url = new DynamicUrl(path, clearHandler);
        }
        return url;
    }

    public NameGen getNameGen() {
        return nameGen;
    }

    public DynamicUrlBuilder setNameGen(NameGen nameGen) {
        this.nameGen = nameGen;
        return this;
    }
}
