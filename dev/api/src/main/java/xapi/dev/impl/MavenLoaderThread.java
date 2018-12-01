package xapi.dev.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.log.X_Log;
import xapi.mvn.api.MvnDependency;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;

import java.util.concurrent.CompletableFuture;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/27/17.
 */
public class MavenLoaderThread extends Thread {

    final IntTo<String> pendingCoords;
    final StringTo<String[]> results;
    private long maxTtl;

    public MavenLoaderThread() {
        super("ReflectiveMavenLoader");
        pendingCoords = X_Collect.newList(String.class, X_Collect.MUTABLE_CONCURRENT);
        results = X_Collect.newStringMap(String[].class, X_Collect.MUTABLE_CONCURRENT);
        setDaemon(true);
    }

    public String[] blockOnResult(String coords) throws InterruptedException {
        String[] paths;
        while ((paths = results.get(coords)) == null) {
            synchronized (results) {
                paths = results.get(coords);
                if (paths == null) {
                    results.wait(getMaxTtl());
                } else {
                    return paths;
                }
            }
        }
        return paths;
    }

    @SuppressWarnings("unused")// called reflectively
    public Out1<String[]> loadCoords(String coords) {
        pendingCoords.add(coords);
        synchronized (this) {
            notifyAll();
        }
        return In1Out1.unsafe(this::blockOnResult).supply(coords);
    }

    @Override
    public void run() {
        long millis = getMaxTtl();
        final long deadline = System.currentTimeMillis() + millis;
        long now;
        while ((now=System.currentTimeMillis()) < deadline) {
            try {
                drainQueuedTasks();
                if (pendingCoords.isEmpty()) {
                    X_Process.flush((int) (deadline - now)/2 + 1);
                }
            } catch (InterruptedException e) {
                X_Log.error(ReflectiveMavenLoader.class, "ReflectiveMavenLoader interrupted; returning immediately");
                return;
            } catch (Exception e) {
                X_Log.error(ReflectiveMavenLoader.class, "Error draining maven downloads", e);
            }
        }
        X_Log.error(ReflectiveMavenLoader.class, "Maven dependencies loading failed after", millis, "ms");
    }


    private void drainQueuedTasks() throws Exception {
        // Called from a thread that definitely has xapi-dev on its classpath.
        // It also definitely does not have access to the classworld(s) who call into us,
        // so we are passing objects in for us to act reflectively upon
        // For each coord we received, we want to start a download from maven...
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Class<?> X_MavenClass = cl.loadClass("xapi.mvn.X_Maven");

        pendingCoords.removeAllUnsafe(coord->{
            final MvnDependency dep = MvnDependency.fromCoords(coord);
            Out1<Iterable<String>> result = (Out1<Iterable<String>>) X_Reflect.invoke(
                X_MavenClass,
                "downloadDependencies",
                new Class[]{MvnDependency.class},
                null,
                dep
            );
            X_Process.blockInBackground(
                CompletableFuture.supplyAsync(result.toSupplier()),
                done->{
                    final String[] path = MappedIterable.mapped(done).toArray(String[]::new);
                    results.put(coord, path);
                    synchronized (results) {
                        results.notifyAll();
                    }
                }
            );
        });

    }

    public long getMaxTtl() {
        return maxTtl == 0 ? 200_000 : maxTtl;
    }

    public void setMaxTtl(long maxTtl) {
        this.maxTtl = maxTtl;
    }
}
