package xapi.dev.impl.dynamic;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.StringTo;
import xapi.dev.api.DynamicUrl;
import xapi.fu.In1Out1;
import xapi.jre.io.AbstractURLStreamHandler;

/**
 * I abhor the name of this class,
 * but it must be called "Handler" in order for java.net.URL to be able to find us.
 *
 * It sucks, but it's not our api.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class Handler extends AbstractURLStreamHandler {

    // TODO: have an injectable service so these values can be distributed across JVMs / classloaders
    // TODO: have a means to listAllFiles, like dynamic:$ls

    private static final StringTo<In1Out1<String, String>> dynamicFiles = X_Collect.newStringMap(In1Out1.class,
        CollectionOptions.asConcurrent(true)
            .mutable(true)
            .insertionOrdered(false)
            .build());

    static {
        addMyPackage(Handler.class);
        registerDynamicUrl(DynamicUrl.LIST_ALL, p->dynamicFiles.keys().join("\n"));
    }

    @Override
    protected StringTo<In1Out1<String, String>> dynamicFiles() {
        return dynamicFiles;
    }

    public static In1Out1<String, String> registerDynamicUrl(String path, In1Out1<String, String> contents) {
        return dynamicFiles.put(path, contents);
    }

    public static void clearDynamicUrl(String path) {
        dynamicFiles.remove(path);
    }

}
