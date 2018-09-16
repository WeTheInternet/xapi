package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.Classpath;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.data.MapLike;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.fu.has.HasLock;
import xapi.fu.iterate.SizedIterable;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.HasScope;
import xapi.scope.api.Scope;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static xapi.server.api.ServerMap.*;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public interface ServerManager <S extends XapiServer> extends HasScope, HasLock {

    default ServerMap map(){
        return getScope().getOrSupply(ServerMap.class, ServerMap::new);
    }

    default <T> StringTo<T> map(String name) {
        return (StringTo<T>) map().multiMaps.get(name);
    }

    default StringTo<IntTo<DoUnsafe>> afterShutdown() {
        return map(KEY_AFTER_SHUTDOWN);
    }

    default IntTo<DoUnsafe> afterShutdown(String serverId) {
        final StringTo<IntTo<DoUnsafe>> map = afterShutdown();
        return map.getOrCreate(serverId, k->X_Collect.newList(DoUnsafe.class));
    }

    default StringTo<IntTo<DoUnsafe>> afterStartup() {
        return map(KEY_AFTER_STARTUP);
    }

    default IntTo<DoUnsafe> afterStartup(String serverId) {
        final StringTo<IntTo<DoUnsafe>> map = afterStartup();
        return map.getOrCreate(serverId, k->X_Collect.newList(DoUnsafe.class));
    }

    default StringTo<IntTo<DoUnsafe>> beforeShutdown() {
        return map(KEY_BEFORE_SHUTDOWN);
    }

    default IntTo<DoUnsafe> beforeShutdown(String serverId) {
        final StringTo<IntTo<DoUnsafe>> map = beforeShutdown();
        return map.getOrCreate(serverId, k->X_Collect.newList(DoUnsafe.class));
    }

    default StringTo<IntTo<DoUnsafe>> beforeStartup() {
        return map(KEY_BEFORE_STARTUP);
    }

    default IntTo<DoUnsafe> beforeStartup(String serverId) {
        final StringTo<IntTo<DoUnsafe>> map = beforeStartup();
        return map.getOrCreate(serverId, k->X_Collect.newList(DoUnsafe.class));
    }

    default StringTo<Boolean> running() {
        return map(KEY_RUNNING);
    }

    default StringTo<WebApp> webApps() {
        return map(KEY_WEBAPPS);
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
        map().servers.forValues(map->map.forValuesUnsafe(XapiServer::shutdown, Do.NOTHING));
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

    default void startup(String name, S app, In1<S> onStartup) {
        IntTo<DoUnsafe> beforeTasks = beforeStartup(name);
        beforeTasks.forEachValue(DoUnsafe::done);

        running().put(name, true);

        app.start(()->{
            onStartup.in(app);

            IntTo<DoUnsafe> afterTasks = afterStartup(name);
            afterTasks.forEachValue(DoUnsafe::done);
        });
    }
    default void shutdown(String name, S app, In1<S> onShutdown) {
        IntTo<DoUnsafe> beforeTasks = beforeShutdown().get(name);
        beforeTasks.forEachValue(DoUnsafe::done);

        running().put(name, false);

        app.shutdown(()->{

            onShutdown.in(app);

            IntTo<DoUnsafe> afterTasks = afterShutdown(name);
            afterTasks.forEachValue(DoUnsafe::done);
        });
    }

    default boolean isRunning(String name) {
        return Boolean.TRUE.equals(running().get(name));
    }


    default S getServer(String name) {
        final StringTo<XapiServer> map = map().servers.getOrCompute(getClass(), c -> X_Collect.newStringMap(XapiServer.class));
        final WebApp classpath = webApps().get(name);
        return (S)map.getOrCreate(name, n->newServer(name, classpath));
    }

    default boolean initializeServer(String name, In1<S> onReady) {
        return initializeServer(name, 0, null, onReady);
    }
    default boolean initializeServer(String name, long time, TimeUnit unit, In1<S> onReady) {
        final S app = getServer(name);

        final Classpath rootCp = app.getWebApp().getClasspaths().get("root");
        assert rootCp != null : "WebApp must have a \"root\" classpath" + app;

        CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = X_Process.newThread(() -> {
            startup(name, app, onReady);
            latch.countDown();
            long maxLatency = 100_000_000L; // 100 ms in nanos
            long wait = 1_000_000; // 1ms min latency
            while (isRunning(name)) {
                LockSupport.parkNanos(wait);
                wait = Math.max(maxLatency, wait * 5 / 4);
                if (Thread.currentThread().isInterrupted()) {
                    X_Log.trace(ServerManager.class, "Releasing interrupted thread; isRunning?", isRunning(name));
                    break;
                }
            }
            // thread is released
            X_Log.trace(ServerManager.class, "Released server", name);
            // We call onRelease from the thread who initialized us.
            // This runs in the classloader of the shutting down server,
            // versus the classloader calling initializeServer
            app.onRelease();
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
        beforeShutdown(name).add(()->{
            // we'll let our daemon know to shutdown at beginning of shutdown
            running().put(name, false);
            // but we'll wait to block until we've started all other shutdown tasks :-)
            afterShutdown(name).add(thread::join);
        });
        try {
            if (unit == null) {
                // wait forever
                latch.await();
                return true;
            } else {
                @SuppressWarnings("UnnecessaryLocalVariable") // nice for debugging
                final boolean success = latch.await(time, unit);
                return success;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default boolean inheritClasspath(S app) {
        return true;
    }

    S newServer(String name, WebApp classpath);

    @Override
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
    static final String KEY_AFTER_STARTUP = "aU";
    static final String KEY_BEFORE_STARTUP = "bU";
    static final String KEY_AFTER_SHUTDOWN = "aD";
    static final String KEY_BEFORE_SHUTDOWN = "bD";
    static final String KEY_RUNNING = "r";
    static final String KEY_WEBAPPS = "a";
    final StringTo<StringTo<Object>> multiMaps = X_Collect.newStringDeepMap(Object.class);
    final ClassTo<StringTo<XapiServer>> servers = X_Collect.newClassMap();
}
