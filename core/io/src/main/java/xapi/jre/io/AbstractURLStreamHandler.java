package xapi.jre.io;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.io.api.StringURLConnection;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public abstract class AbstractURLStreamHandler extends URLStreamHandler {

    protected abstract StringTo<In1Out1<String, String>> dynamicFiles();

    protected static void addMyPackage(Class<? extends URLStreamHandler> handlerClass) {
        // Ensure that we are registered as a url protocol handler for JavaFxCss:/path css files.
        String was = System.getProperty("java.protocol.handler.pkgs", "");
        String pkg = handlerClass.getPackage().getName();
        int ind = pkg.lastIndexOf('.');
        assert ind != -1 : "You can't add url handlers in the base package";
        assert "Handler".equals(handlerClass.getSimpleName()) : "A URLStreamHandler must be in a class named Handler; not " + handlerClass.getSimpleName();

        System.setProperty("java.protocol.handler.pkgs", handlerClass.getPackage().getName().substring(0, ind) +
            (was.isEmpty() ? "" : "|" + was ));
    }


    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        final String path = u.getPath();
        final In1Out1<String, String> file = dynamicFiles().get(path);
        return new StringURLConnection(u, file.supply(path));
    }
}
