package xapi.dev.api;

import xapi.dev.source.NameGen;
import xapi.fu.Do;

/**
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class DynamicUrl {

    public static final String LIST_ALL = "$ls";
    public static final String LIST_ALL_URL = "dynamic:$ls";

    private final String path;
    private final Do clearHandler;

    public DynamicUrl(NameGen nameGen) {
        this(nameGen, Do.NOTHING);
    }

    public DynamicUrl(String path) {
        this(path, Do.NOTHING);
    }

    public DynamicUrl(NameGen nameGen, Do clearHandler) {
        this(NameGen.notNull(nameGen).newName("d/"), clearHandler);
    }

    public DynamicUrl(String path, Do clearHandler) {
        this.path = path;
        assert clearHandler != null : "Null clearHandler not supported";
        this.clearHandler = clearHandler;
    }

    public String getUrl() {
        return "dynamic:" + path;
    }

    public void clear() {
        clearHandler.done();
    }

    public Do getClearHandler() {
        return clearHandler;
    }
}
