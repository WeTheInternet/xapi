package xapi.test.server.bdd;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.Rethrowable;
import xapi.process.X_Process;
import xapi.dev.api.Classpath;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.gen.WebAppGenerator;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertNotNull;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public interface ServerTestHelper <ServerType extends XapiServer> extends Rethrowable {

    // Yes, these are static; it's for testing only, and retrofit disallows super classes,
    // so we live with this ugliness to be able to inherit server-related functionality
    StringTo<WebApp> webApps = X_Collect.newStringMap(WebApp.class);
    IntTo<DoUnsafe> cleanups = X_Collect.newList(DoUnsafe.class);
    StringTo<IntTo<DoUnsafe>> startups = X_Collect.newStringMap(IntTo.class);
    Mutable<Out1<WebAppGenerator>> generatorFactory = new Mutable<>(WebAppGenerator::new);
    ClassTo<StringTo<XapiServer>> servers = X_Collect.newClassMap(StringTo.class);
    StringTo<Boolean> running = X_Collect.newStringMap(Boolean.class);

    default void cleanup() {
        running.clear();
        cleanups.forEachValue(DoUnsafe::done);
        cleanups.clear();
        webApps.clear();
        servers.forValues(map->map.forValuesUnsafe(XapiServer::shutdown, Do.NOTHING));
        servers.clear();
    }

    default void startup(String name) {
        final IntTo<DoUnsafe> tasks = startups.get(name);
        if (tasks != null) {
            tasks.forEachValue(DoUnsafe::done);
        }
        running.put(name, true);
    }

    default WebApp createWebApp(String name, UiContainerExpr app) {
        WebAppGenerator generator = createGenerator();
        generator.setUiComponent(false);
        WebApp classpath = generator.generateWebApp(name, app, callback->{

            final DoUnsafe doInstall = ()->{
                final ServerType server = getServer(name);
                callback.in(server);
            };
            if (isRunning(name)) {
                doInstall.done();
            } else {
                startups
                    .getOrCreate(name, n->X_Collect.newList(DoUnsafe.class))
                    .add(doInstall);
            }
        });
        classpath.setContentRoot(classpath.getContentRoot().replace("generated-sources", "generated-test-sources"));
        webApps.put(name, classpath);
        return classpath;
    }

    default boolean isRunning(String name) {
        return Boolean.TRUE.equals(running.get(name));
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
        CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = X_Process.newThread(() -> {
            startup(name);
            onReady.in(app);
            latch.countDown();
            while (isRunning(name)) {
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
            running.put(name, false);
            thread.join();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default boolean inheritClasspath(ServerType app) {
        return true;
    }

    ServerType newServer(String name, WebApp classpath);

}
