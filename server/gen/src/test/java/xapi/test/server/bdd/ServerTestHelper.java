package xapi.test.server.bdd;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.Rethrowable;
import xapi.process.X_Process;
import xapi.server.api.Classpath;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.gen.WebAppGenerator;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public interface ServerTestHelper <ServerType extends XapiServer> extends Rethrowable {

    StringTo<WebApp> webApps = X_Collect.newStringMap(WebApp.class);
    IntTo<DoUnsafe> cleanups = X_Collect.newList(DoUnsafe.class);
    Mutable<Out1<WebAppGenerator>> generatorFactory = new Mutable<>(WebAppGenerator::new);
    ClassTo<StringTo<XapiServer>> servers = X_Collect.newClassMap(StringTo.class);

    default void cleanup() {
        cleanups.forEachValue(DoUnsafe::done);
        cleanups.clear();
        webApps.clear();
        servers.forValues(map->map.forValuesUnsafe(XapiServer::shutdown));
        servers.clear();
    }

    default WebApp createWebApp(String name, UiContainerExpr app) {
        WebAppGenerator generator = createGenerator();
        WebApp classpath = generator.generateWebApp(name, app);
        webApps.put(name, classpath);
        return classpath;
    }

    default WebAppGenerator createGenerator() {
        return generatorFactory.out1().out1();
    }

    default ServerType getServer(String name) {
        final StringTo<XapiServer> map = servers.getOrCompute(getClass(), c -> X_Collect.newStringMap(XapiServer.class));
        final WebApp classpath = webApps.get(name);
        return (ServerType)map.getOrCreate(name, n->newServer(name, classpath));
    }

    default void initializeServer(String name, In1<ServerType> onReady) {
        final ServerType app = getServer(name);

        final Classpath rootCp = app.getWebApp().getClasspaths().get("root");
        assertNotNull("WebApp must have a \"root\" classpath" + app, rootCp);
        Mutable<Boolean> running = new Mutable<>(true);
        CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = X_Process.newThread(() -> {
            onReady.in(app);
            latch.countDown();
            while (running.out1()) {
                LockSupport.parkNanos(500_000);
            }
        });
        final IntTo<String> paths = rootCp.getPaths();
        final URL[] urls = new URL[paths.size()];
        paths.forEachPairUnsafe((i, v)->
            urls[i] = new URL(v)
        );
        final ClassLoader cl = new URLClassLoader(urls, inheritClasspath(app) ? Thread.currentThread().getContextClassLoader() : null);
        thread.setContextClassLoader(cl);
        thread.setDaemon(true);
        thread.start();
        cleanups.add(()->{
            running.in(false);
           thread.join();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
    }

    default boolean inheritClasspath(ServerType app) {
        return true;
    }

    ServerType newServer(String name, WebApp classpath);

}
