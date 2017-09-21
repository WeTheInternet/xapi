package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.Classpath;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.MapLike;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.fu.has.HasLock;
import xapi.fu.iterate.SizedIterable;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.HasScope;
import xapi.scope.api.Scope;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public interface ServerManager <S extends XapiServer> extends HasScope, HasLock {

    default ServerMap map(){
        return getScope().getOrSupply(ServerMap.class, ServerMap::new);
    }

    default <T> StringTo<T> map(String name) {
        return (StringTo<T>) map().stringMaps.get(name);
    }

    default StringTo<IntTo<DoUnsafe>> afterShutdown() {
        return map("aD");
    }

    default StringTo<IntTo<DoUnsafe>> afterStartup() {
        return map("aU");
    }

    default StringTo<IntTo<DoUnsafe>> beforeShutdown() {
        return map("bD");
    }

    default StringTo<IntTo<DoUnsafe>> beforeStartup() {
        return map("bU");
    }

    default StringTo<Boolean> running() {
        return map("r");
    }

    default StringTo<WebApp> webApps() {
        return map("a");
    }

    default <C extends S> S server(Class<C> serverType, String serverName) {
        final StringTo<XapiServer> map = map().servers.getOrCompute(serverType, c->X_Collect.newStringMap(serverType));
        return (S) map.get(serverName);
    }

    default void cleanup() {
        running().clear();

        // Everything registered to run before shutdown goes here
        doAll(beforeShutdown());

        // Directly allow all running servers to shutdown
        map().servers.forValues(map->map.forValuesUnsafe(XapiServer::shutdown));
        // In case implementation shutdown called generic code that added beforeShutdown code, clear it again
        doAll(beforeShutdown());
        doAll(afterShutdown());
        assert beforeShutdown().isEmpty() :
            "An afterShutdown() task added a beforeShutdown() callback that will never be invoked: " +  beforeShutdown();

        map().servers.clear();
        webApps().clear();
    }

    default void doAll(MapLike<?, ? extends SizedIterable<DoUnsafe>> all) {

        final Out1<DoUnsafe[]> removeItems = ()->{
            // Extract a flattened array of tasks
            final DoUnsafe[] items = all.forEachItem()
                .map(Out2::out2)
                .filterNull()
                .flatten(X_Fu::<Iterable<DoUnsafe>>identity)
                .toArray(DoUnsafe[]::new);
            // Clear out the collection so it can be used by others.
            all.clear();
            return items;
        };
        int recursionSickness = maxLooping();
        while (all.isNotEmpty() && recursionSickness --> 0) {

            final DoUnsafe[] tasks = mutex(removeItems);

            for (DoUnsafe task : tasks) {
                task.done();
            }
        }
        assert recursionSickness > 0 : "Ran out of recursion for completion of tasks in " + all +".\n\n" +
            "You appear to have livelock from a callback putting itself (directly or indirectly) back onto" +
            " the stack trace from this assertion";

    }

    default int maxLooping() {
        return 100;
    }

    default void startup(String name, S app, In1<S> onReady) {
        IntTo<DoUnsafe> tasks = beforeStartup().get(name);
        if (tasks != null) {
            tasks.forEachValue(DoUnsafe::done);
        }
        running().put(name, true);
        onReady.in(app);

        tasks = afterStartup().get(name);
        if (tasks != null) {
            tasks.forEachValue(DoUnsafe::done);
        }

    }

    default boolean isRunning(String name) {
        return Boolean.TRUE.equals(running().get(name));
    }


    default S getServer(String name) {
        final StringTo<XapiServer> map = map().servers.getOrCompute(getClass(), c -> X_Collect.newStringMap(XapiServer.class));
        final WebApp classpath = webApps().get(name);
        return (S)map.getOrCreate(name, n->newServer(name, classpath));
    }

    default void initializeServer(String name, In1<S> onReady) {
        final S app = getServer(name);

        final Classpath rootCp = app.getWebApp().getClasspaths().get("root");
        assert rootCp != null : "WebApp must have a \"root\" classpath" + app;

        CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = X_Process.newThread(() -> {
            startup(name, app, onReady);
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
        beforeShutdown().get(name).add(()->{
            // we'll let our daemon know to shutdown at beginning of shutdown
            running().put(name, false);
            // but we'll wait to block until we've started all other shutdown tasks :-)
            afterShutdown().get(name).add(thread::join);
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default boolean inheritClasspath(S app) {
        return true;
    }

    S newServer(String name, WebApp classpath);

    default Scope getScope() {
        return X_Scope.currentScope().findParent(GlobalScope.class, false)
            .getOrThrow(()->new IllegalStateException("No GlobalScope"));
    }

}

/**
 * We want to put our data inside a foreign Scope object,
 * so, we should be careful to use keys only we know about.
 * Scopes use classes as keys for types,
 * thus, we create this package-protected type to hold all of our state
 * so we don't overwrite anything owned by anyone else in the current scope.
 */
final class ServerMap {
    final StringTo<StringTo<Object>> stringMaps = X_Collect.newStringDeepMap(Object.class);
    final ClassTo<StringTo<XapiServer>> servers = X_Collect.newClassMap();
}
