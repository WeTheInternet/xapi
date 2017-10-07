package xapi.jre.ui.css;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.StringTo;
import xapi.fu.Out1;
import xapi.io.api.StringURLConnection;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * I abhor the name of this class,
 * but it must be called "Handler" in order for java.net.URL to be able to find us.
 *
 * It sucks, but it's not our api, and it's the only way to get dynamic stylesheets in JavaFx,
 * short of overriding the url stream handler directly (and this can only be done once in a single
 * JVM, and as framework-level code, it is unacceptable to prevent clients from choosing to
 * override the stream handler themselves).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/21/16.
 */
public class Handler extends URLStreamHandler {

    private static final StringTo<Out1<String>> dynamicFiles;
    static {
        // Ensure that we are registered as a url protocol handler for JavaFxCss:/path css files.
        String was = System.getProperty("java.protocol.handler.pkgs", "");
        System.setProperty("java.protocol.handler.pkgs", Handler.class.getPackage().getName().replace(".css", "") +
            (was.isEmpty() ? "" : "|" + was ));
        dynamicFiles = X_Collect.newStringMap(Out1.class,
            CollectionOptions.asConcurrent(true)
                .mutable(true)
                .insertionOrdered(false)
            .build());
    }

    public static String registerStylesheet(String path, Out1<String> contents) {
        dynamicFiles.put(path, contents);
        return path;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        final String path = u.getPath();
        final Out1<String> file = dynamicFiles.get(path);
        String query = u.getQuery();
        if (query != null) {
            // parse the query params, and send them to the css builder (use something other than Out1)
        }
        return new StringURLConnection(u, file);
    }

}
