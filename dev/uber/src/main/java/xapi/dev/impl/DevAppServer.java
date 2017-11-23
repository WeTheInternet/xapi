package xapi.dev.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.log.X_Log;
import xapi.server.api.ServerManager;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.gen.VertxWebAppGenerator;
import xapi.server.vertx.XapiVertxServer;
import xapi.time.X_Time;
import xapi.time.api.Moment;

import java.net.URL;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class DevAppServer implements ServerManager<XapiVertxServer> {

    private WeakHashMap<XapiVertxServer, WebApp> servers = new WeakHashMap<>();

    public void shutdown(DoUnsafe onDone) {
        for (Entry<XapiVertxServer, WebApp> e : servers.entrySet()) {
            if (e.getKey() != null) {
                e.getKey().shutdown(Do.NOTHING);
            }
            if (e.getValue() != null) {
                e.getValue().shutdown();
            }
        }
        onDone.done();
    }

    @Override
    public XapiVertxServer newServer(String name, WebApp classpath) {
        final XapiVertxServer server = new XapiVertxServer(classpath);
        servers.put(server, classpath);
        return server;
    }


    protected boolean doStartServer(String serverName, int port) {
        final Moment start = X_Time.now();
        X_Log.info(DevApp.class, "Starting server");
        VertxWebAppGenerator generator = new VertxWebAppGenerator(s -> {
            // we'll leave the /gwt folder for compiled client stuff...
            s.setGenerateDirectory(s.getGenerateDirectory().replace("/gwt", "/annotations"));
            s.setImplicitMode(ImplicitMode.CLASS);
            return s;
        });
        final UiContainerExpr ui;
        final URL res = getClass().getResource(serverName + ".xapi");
        try {
            ui = JavaParser.parseXapi(res.openStream());
        } catch (Exception e) {
            X_Log.error(DevApp.class, "Could not load server xapi file", serverName,  e);
            throw X_Fu.rethrow(e);
        }

        final Mutable<In1<XapiServer<?>>> install = new Mutable<>();
        final WebApp app = generator.generateWebApp(serverName, ui, install);
        app.setPort(port);
        webApps().put(serverName, app);
        if (isRunning(serverName)) {
            final XapiVertxServer server = getServer(serverName);
            install.out1().in(server);
        } else {
            initializeServer(serverName, done -> {
                install.out1().in(done);
                done.start(()->X_Log.info(DevApp.class,
                    serverName + " online @ port ", port, "in", X_Time.difference(start)));
            });
        }
        return true;
    }

}
