package xapi.dev.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.X_Fu;
import xapi.fu.has.HasLock;
import xapi.io.X_IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static java.security.AccessController.doPrivileged;
import static xapi.collect.X_Collect.MUTABLE_CONCURRENT;

/**
 * A {@link URLClassLoader} subclass that is designed to load resources from extensible sources.
 *
 * Technically being mutable is a bit scary for something like a ClassLoader,
 * but if you perform all mutation before using the loader, you will be safe.
 *
 * To enforce this, call {@link #freeze()} to throw exceptions on future mutation.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class ExtensibleClassLoader extends URLClassLoader implements HasLock {

    private volatile boolean checkMeFirst;
    private volatile boolean frozen;

    private volatile In1Out1<String, URL> resourceFinder = In1Out1.returnNull();
    private volatile In1Out1<String, Class<?>> classFinder = In1Out1.returnNull();

    private final StringTo<Class<?>> loadedClasses = X_Collect.newStringMap(Class.class, MUTABLE_CONCURRENT);
    private final StringTo<URL> loadedResources = X_Collect.newStringMap(URL.class, MUTABLE_CONCURRENT);
    private final Lazy<DynamicUrl> myUrl = Lazy.deferred1(()->{
        DynamicUrl url = new DynamicUrl("cl" + System.identityHashCode(this));
        url.withValue(this::getResourceUtf8);
        return url;
    });

    public ExtensibleClassLoader() {
        this(new URL[0], null);
    }

    public ExtensibleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public ExtensibleClassLoader(URL[] urls) {
        super(urls);
    }

    public ExtensibleClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public URL getResource(String name) {
        URL result;
        if (checkMeFirst) {
            result = loadedResources.getOrCreate(name, resourceFinder);
            if (result == null) {
                result = super.getResource(name);
                if (result != null && cacheForeignResources()) {
                    loadedResources.put(name, result);
                }
            }
        } else {
            result = super.getResource(name);
            if (result == null) {
                result = loadedResources.getOrCreate(name, resourceFinder);
            } else if (cacheForeignResources()){
                loadedResources.put(name, result);
            }
        }
        return result;
    }

    public String getResourceUtf8(String name) {
        final URL url = getResource(name);
        if (url == null) {
            return null;
        }
        try {
            return X_IO.toStringUtf8(url.openConnection().getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected boolean cacheForeignResources() {
        return true;
    }

    protected boolean cacheForeignClasses() {
        return cacheForeignResources();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // class loading already has an implicit locking on each name.
        synchronized (getClassLoadingLock(name)) {
            Class<?> result;
            if (checkMeFirst) {
                // only consult the map if we're supposed to check ourselves first
                result = loadedClasses.getOrCreate(name, classFinder);
                if (result == null) {
                    if (!name.startsWith("java.")) {
                        result = findClass(name);
                    }
                    if (result == null) {
                        result = super.loadClass(name, false);
                        if (result != null && cacheForeignClasses()) {
                            loadedClasses.put(name, result);
                        }
                    } else if (cacheForeignClasses()){
                        loadedClasses.put(name, result);
                    }
                }
            } else {
                // consult the super loader first
                try {
                    result = super.loadClass(name, false);
                } catch (ClassNotFoundException e) {
                    result = loadedClasses.getOrCreate(name, classFinder);
                }
                if (result != null && cacheForeignClasses()){
                    loadedClasses.put(name, result);
                }
            }
            if (result == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(result);
            }
            return result;
        }
    }

    @Override
    public URL[] getURLs() {
        final URL[] givenUrls = super.getURLs();
        String dynamicUrl = getDynamicUrl();
        try {
            return X_Fu.push(givenUrls, new URL(dynamicUrl));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> result;
        try {
            result = doPrivileged((PrivilegedExceptionAction<Class<?>>) ()->{
                String path = name.replace('.', '/').concat(".class");
                // We want class loading to access our custom resource paths.
                URL res = getResource(path);
                if (res == null) {
                    throw new ClassNotFoundException(name);
                } else {
                    try (
                        final InputStream connection = res.openConnection().getInputStream()
                    ){
                        final byte[] bytes = X_IO.toByteArray(connection);
                        return defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw (ClassNotFoundException)e.getException();
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    @Override
    public URL findResource(String name) {
        // getResource and findResource have different semantics;
        // findResources is, by nature, a local-only lookup,
        // while getResource checks parent classloaders;
        // to make checkMeFirst work correctly, we must override both.
        URL result;
        if (checkMeFirst) {
            result = loadedResources.getOrCreate(name, resourceFinder);
            if (result == null) {
                result = super.findResource(name);
                if (result != null && cacheForeignResources()) {
                    loadedResources.put(name, result);
                }
            }
        } else {
            result = super.findResource(name);
            if (result == null) {
                result = loadedResources.getOrCreate(name, resourceFinder);
            } else if (cacheForeignResources()){
                loadedResources.put(name, result);
            }
        }
        return result;
    }

    public boolean isCheckMeFirst() {
        return checkMeFirst;
    }

    public void setCheckMeFirst(boolean checkMeFirst) {
        checkNotFrozen();
        this.checkMeFirst = checkMeFirst;
    }

    private void checkNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("frozen");
        }
    }

    public final ExtensibleClassLoader addResourceFinder(In1Out1<String, URL> finder) {
        checkNotFrozen();
        resourceFinder = mutex(()->
            resourceFinder == In1Out1.RETURN_NULL ? finder
                : resourceFinder.mapIfNull(finder)
        );
        return this;
    }

    public final ExtensibleClassLoader setResourceFinder(In1Out1<String, URL> finder) {
        checkNotFrozen();
        resourceFinder = mutex(Immutable.immutable1(finder));
        return this;
    }

    public final ExtensibleClassLoader addClassFinder(In1Out1<String, Class<?>> finder) {
        checkNotFrozen();
        classFinder = mutex(()->
            classFinder == In1Out1.RETURN_NULL ? finder
                : classFinder.mapIfNull(finder)
        );
        return this;
    }

    public final ExtensibleClassLoader setClassFinder(In1Out1<String, Class<?>> finder) {
        checkNotFrozen();
        classFinder = mutex(Immutable.immutable1(finder));
        return this;
    }

    public final ExtensibleClassLoader checkMeFirst() {
        setCheckMeFirst(true);
        return this;
    }

    public ExtensibleClassLoader freeze() {
        // consider checking not already frozen?  seems draconian...
        frozen = true;
        return this;
    }

    public ExtensibleClassLoader melt() {
        frozen = false;
        return this;
    }

    public ExtensibleClassLoader clear() {
        checkNotFrozen();
        loadedClasses.clear();
        loadedResources.clear();
        return this;
    }

    protected String getDynamicUrl() {
        return myUrl.out1().getUrl();
    }
}
