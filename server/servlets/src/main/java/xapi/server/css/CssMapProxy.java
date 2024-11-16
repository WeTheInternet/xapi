package xapi.server.css;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.log.X_Log;
import xapi.ui.api.style.CssProxy;
import xapi.util.X_Util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.*;

import com.google.gwt.resources.client.ClientBundle;

/**
 * Used to translate a gwt-generated .cssmap file into a workable proxy interface.
 *
 * This class is responsible for producing proxies backed by these compiled outputs.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/9/17.
 */
public class CssMapProxy <Css> implements CssProxy<Css>{
    private static final WatchService watcher;

    static {
        WatchService service;
        try {
            service = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            X_Log.error(CssMapProxy.class, "Unable to create file watch service; will not be able to hot reload classname changes");
            service = null;
        }
        watcher = service;
    }

    private final Lazy<Css> proxy;
    private final Class<? extends Css> cssType;
    private Path mapFile;
    private Do onMapFileChanged;
    private final StringTo<String> knownNames;
    private WatchKey key;

    private class ProxyHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            maybeRefreshNames();
            switch (method.getName()) {
                case "proxyCacheKey":
                    // When trying to cache results, we want to compute a proxyCacheKey that will
                    // uniquely identify our current settings.
                    return proxyCacheKey();
                case "maybeRefreshNames":
                    maybeRefreshNames();
                    return null;
                case "getProxy":
                    return getProxy();
            }
            synchronized (knownNames) {
                return knownNames.getOrCreateUnsafe(method.getName(), name->{
                    throw new IllegalArgumentException("No css classname found for " + name + " in known names:\n|" + knownNames +"|");
                });
            }
        }

    }

    protected void doAfterNameRefresh(String oldCacheKey, String newCacheKey, StringTo<String> afterModification) {
    }

    protected void doBeforeNameRefresh(String oldCacheKey, StringTo<String> beforeModification) {
    }

    private final ProxyHandler handler;
    private String cacheKey;

    public CssMapProxy(String cssMapRoot, Class<? extends ClientBundle> resourceType, String methodName, Class<? extends Css> cssType) {
        this.proxy = Lazy.deferred1(this::createProxy);
        this.mapFile = Paths.get(cssMapRoot, resourceType.getCanonicalName() + "." + methodName + ".cssmap");
        this.cssType = cssType;
        onMapFileChanged = Do.NOTHING;
        knownNames= X_Collect.newStringMap(String.class);
        this.handler = new ProxyHandler();
        refreshNames();
        watchFile();

    }

    protected Css createProxy() {

        final Object inst = Proxy.newProxyInstance(getClassLoader(), new Class[]{
            cssType, CssProxy.class
        }, handler);
        return (Css) inst;
    }

    protected ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public String proxyCacheKey() {
        return mapFile.toAbsolutePath().toString() + " : " + knownNames.forEachValue().join(" ");
    }

    @Override
    public void maybeRefreshNames() {
        if (key != null && !key.pollEvents().isEmpty()) {
            refreshNames();
        }
    }

    private void watchFile() {
        if (watcher == null) {
            key = null;
        } else {
            if (key != null) {
                key.cancel();
            }
            try {
                key = mapFile.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                onMapFileChanged = onMapFileChanged.doAfter(key::cancel);
            } catch (IOException e) {
                X_Log.warn(
                    CssMapProxy.class,
                    "Unable to watch css files for changes; expect server side rendering " +
                        "to become stale if you are recompiling changes",
                    e
                );
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Performs the "heavy lifting" of parsing the names out of the file, and into our map
     */
    private void refreshNames() {
        final String oldCacheKey = cacheKey;
        doBeforeNameRefresh(oldCacheKey, knownNames);
        synchronized (knownNames) {
            knownNames.clear();
            try {
                String prefix = cssType.getCanonicalName().replace('.', '-') + '-';
                Files.lines(mapFile)
                    .forEach(line->{
                        String[] bits = line.replace(prefix, "").split(",");
                        knownNames.put(bits[0], bits[1]);
                    });
            } catch (IOException e) {
                X_Log.warn(CssMapProxy.class, "Unable to refresh names from file " + mapFile);
                throw new UncheckedIOException(e);
            }
            cacheKey = proxyCacheKey();
        }
        doAfterNameRefresh(oldCacheKey, cacheKey, knownNames);
    }

    @Override
    public Css getProxy() {
        return proxy.out1();
    }

    @Override
    public boolean sameProxy(Css css) {
        if (css instanceof CssProxy) {
            CssProxy proxy = (CssProxy) css;
            // "your" proxy cache key is the current set of names that will be used for this proxy,
            String yourKey = proxy.proxyCacheKey();
            // We'll check that against the old cache key from the last time we updated...
            return X_Util.equal(yourKey, cacheKey);
        }
        return false;
    }

    /**
     * Call this if you recompile gwt into a new output folder (like super-dev-mode).
     */
    @Override
    public void resetProxy(String newMapRoot) {
        final Path newPath = Paths.get(newMapRoot);
        if (!mapFile.equals(newPath)) {
            onMapFileChanged.done();
            onMapFileChanged = Do.NOTHING;
            this.mapFile = newPath;
            refreshNames();
            watchFile();
        }
    }
}
