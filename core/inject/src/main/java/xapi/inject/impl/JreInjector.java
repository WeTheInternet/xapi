package xapi.inject.impl;

import xapi.collect.api.InitMap;
import xapi.collect.impl.AbstractInitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.inject.api.Injector;
import xapi.inject.api.PlatformChecker;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;
import xapi.util.X_Runtime;
import xapi.util.api.ConvertsValue;
import xapi.util.impl.ImmutableProvider;

import static xapi.util.X_Namespace.*;

import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class JreInjector implements Injector {

    static final String DEFAULT_INJECTOR = "xapi.jre.inject.RuntimeInjector";

    private static final class RuntimeProxy
        extends SingletonProvider<In2<String, PlatformChecker>>
        implements In2<String, PlatformChecker> {
        @SuppressWarnings("unchecked")
        @Override
        protected In2<String, PlatformChecker> initialValue() {
            final String injector = System.getProperty(
                PROPERTY_INJECTOR,
                DEFAULT_INJECTOR
            );
            try {
                final Class<?> cls =
                    Class.forName(injector);
                return (In2<String, PlatformChecker>) cls.newInstance();
            } catch (final ClassNotFoundException e) {
                X_Log.warn(getClass(), "Unable to find injector ", injector, "on the classpath");
            } catch (final Exception e) {
                e.printStackTrace();
                final Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, e);
            }
            return this;

        }

        @Override
        public void in(final String value, PlatformChecker checker) {
            //no-op implementation.

        }
    }

    private static final RuntimeProxy runtimeInjector = new RuntimeProxy();

    private boolean initOnce = true;

    private final PlatformChecker checker;

    public JreInjector() {
        checker = createPlatformChecker();
        if (checker.needsInject()) {
            scanClasspath();
        }
    }

    private static final SingletonProvider<String> instanceUrlFragment
        = new SingletonProvider<String>() {
        @Override
        protected String initialValue() {
            final String value = System.getProperty(
                PROPERTY_INSTANCES,
                DEFAULT_INSTANCES_LOCATION
            );
            return value.endsWith("/") ? value : value + "/";
        }

        ;
    };
    private static final SingletonProvider<String> singletonUrlFragment
        = new SingletonProvider<String>() {
        @Override
        protected String initialValue() {
            final String value = System.getProperty(
                PROPERTY_SINGLETONS,
                DEFAULT_SINGLETONS_LOCATION
            );
            return value.endsWith("/") ? value : value + "/";
        }

        ;
    };

    private final AbstractInitMap<Class<?>, Provider<?>> instanceProviders
        = InitMapDefault.createInitMap(
        AbstractInitMap.CLASS_NAME,
        new In1Out1<Class<?>, Provider<?>>() {

            @Override
            public Provider<?> io(Class<?> clazz) {
                //First, lookup META-INF/instances for a replacement.
                final Class<?> cls;
                try {
                    cls = lookup(clazz, instanceUrlFragment.get(), JreInjector.this, instanceProviders);
                    if (cls == clazz) {
                        if (instanceProviders.containsKey(cls)) {
                            return instanceProviders.get(cls);
                        } else if (singletonProviders.containsKey(clazz)) {
                            X_Log.warn(getClass(), "Attempting instance injection for ", clazz
                                , "but no instance type registered; however, there is a singleton type available.",
                                "Consider using X_Inject(" + clazz.getSimpleName() + ".class) instead"
                            );
                        }
                    }
                    return () -> {
                        try {
                            return cls.newInstance();
                        } catch (final Exception e) {
                            e.printStackTrace();
                            String message = "Could not instantiate new instance of " + cls.getName() + " : " + clazz;
                            throw new RuntimeException(message, e);
                        }
                    };
                } catch (final Exception e) {
                    if (instanceProviders.containsKey(clazz)) {
                        return instanceProviders.get(clazz);
                    }
                    throw new RuntimeException(
                        "Could not create instance provider for " + clazz.getName() + " : " + clazz,
                        e
                    );
                }
            }
        }
    );
    /**
     * Rather than use java.util.ServiceLoader, which only works in Java >= 6,
     * and which can cause issues with android+proguard,
     * we use our own simplified version of ServiceLoader,
     * which can allow us to change the target directory from META-INF/singletons,
     * and caches singletons internally.
     * <p>
     * Note that this method will use whatever ClassLoader loaded the key class.
     */
    private final InitMap<Class<?>, Provider<Object>> singletonProviders =
        InitMapDefault.createInitMap(AbstractInitMap.CLASS_NAME, new
            In1Out1<Class<?>, Provider<Object>>() {
                @Override
                public Provider<Object> io(final Class<?> clazz) {
                    //TODO: optionally run through java.util.ServiceLoader,
                    //in case client code already uses ServiceLoader directly (unlikely edge case)
                    Class<?> cls;
                    try {
                        //First, lookup META-INF/singletons for a replacement.
                        cls = lookup(clazz, singletonUrlFragment.get(), JreInjector.this, singletonProviders);
                        if (cls == clazz && singletonProviders.containsKey(cls)) {
                            return singletonProviders.get(cls);
                        }
                        return new ImmutableProvider<>(cls.newInstance());
                    } catch (final Throwable e) {
                        if (singletonProviders.containsKey(clazz)) {
                            return singletonProviders.get(clazz);
                        }
                        //Try to log the exception, but do not recurse into X_Inject methods
                        if (clazz == LogService.class) {
                            final LogService serv = new JreLog();
                            final ImmutableProvider<Object> provider = new ImmutableProvider<Object>(serv);
                            singletonProviders.setValue(clazz.getName(), provider);
                            return provider;
                        }
                        e.printStackTrace();
                        String message = "Could not instantiate singleton for " + clazz.getName() + " for " + clazz;
                        if (instanceProviders.containsKey(clazz)) {
                            message += "\nThe type " + clazz + " does have a mapping for an instance scope;\n" +
                            "use X_Inject.instance(" + clazz.getSimpleName()+".class) instead";
                        }
                        tryLog(message, e);
                        throw new RuntimeException(message, e);
                    }

                }
            });

    protected PlatformChecker createPlatformChecker() {
        return new PlatformChecker();
    }

    private void tryLog(final String message, final Throwable e) {
        try {
            final LogService log = (LogService) singletonProviders.get(LogService.class).get();
            log.log(LogLevel.ERROR, message);
        } catch (final Exception ex) {
            System.err.println(message);
            ex.printStackTrace();
        }
    }

    private static Class<?> lookup(
        final Class<?> cls,
        String relativeUrl,
        final JreInjector injector,
        final InitMap<Class<?>, ?> map
    )
    throws IOException {
        final String name = cls.getName();
        final ClassLoader loader = getBestLoader(cls.getClassLoader());
        if (!relativeUrl.endsWith("/")) {
            relativeUrl += "/";
        }
        Enumeration<URL> resources = loader.getResources(relativeUrl + name);
        if (resources == null || !resources.hasMoreElements()) {
            if (injector.initOnce) {
                injector.initOnce = false;
                injector.init(cls, map);
                resources = loader.getResources(relativeUrl + name);
                if (relativeUrl.contains(instanceUrlFragment.get())) {
                    if (injector.instanceProviders.containsKey(cls)) {
                        return cls;
                    }
                } else {
                    if (injector.singletonProviders.containsKey(cls)) {
                        return cls;
                    }
                }
            }
            if (resources == null || !resources.hasMoreElements()) {
                return cls;
            }
        }
        URL resource;
        Map<Class<?>, Integer> candidates = new LinkedHashMap<>();
        while (resources.hasMoreElements()) {
            resource = resources.nextElement();
            final InputStream stream = resource.openStream();
            final byte[] into = new byte[stream.available()];
            stream.read(into);
            try {
                String result = new String(into).split("\n")[0];
                String[] bits = result.split("=");
                Class<?> clazz;
                try {
                    final Class<?> fromKeyLoader = Class.forName(bits[0], true, cls.getClassLoader());

                    clazz = fromKeyLoader;
                } catch (ClassNotFoundException e) {
                    try {
                        final ClassLoader threadCl = Thread.currentThread().getContextClassLoader();
                        if (cls.getClassLoader() == threadCl) {
                            continue;
                        }
                        final Class<?> fromThreadLoader = Class.forName(
                            bits[0],
                            true,
                            threadCl
                        );
                        clazz = fromThreadLoader;
                    } catch (ClassNotFoundException ex) {
                        // Nope.  class is nowhere to be found...
                        continue;
                    }
                }

                Integer newVal = bits.length == 1 ? null : Integer.parseInt(bits[1].trim());
                final Integer was = candidates.get(clazz);
                if (was == null) {
                    candidates.put(clazz, newVal);
                } else if (newVal != null) {
                    candidates.put(clazz, Math.max(was, newVal));
                }
            } finally {
                stream.close();
            }
        }
        Class<?> best = injector.checker.findBest(candidates);
        if (best == null) {
            // TODO: warn
            return cls;
        }
        return best;
    }

    private static ClassLoader getBestLoader(ClassLoader cl) {
        final ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (cl == context) {
            return cl;
        }
        if (isParent(cl, context)) {
            return context;
        }
        return cl;
    }

    private static boolean isParent(ClassLoader cl, ClassLoader context) {
        while (context != null) {
            if (context.getParent() == cl) {
                return true;
            }
            context = context.getParent();
        }
        return false;
    }

    protected static boolean isAcceptable(Class<?> clazz) {

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C extends Class<? extends T>> T create(final C cls) {
        try {
            return (T) instanceProviders.get(cls).get();
        } catch (final Exception e) {
            if (initOnce) {
                X_Log.warn("Instance provider failed; attempting runtime injection", e);
                initOnce = false;
                init(cls, instanceProviders);
                return create(cls);
            }
            //Try to log the exception, but do not recurse into X_Inject methods
            final String message = "Could not instantiate instance for " + cls.getName();
            tryLog(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C extends Class<? extends T>> T provide(final C cls) {
        try {
            return (T) singletonProviders.get(cls).get();
        } catch (final Exception e) {
            if (initOnce) {
                X_Log.warn("Singleton provider failed; attempting runtime injection", e);
                initOnce = false;
                init(cls, singletonProviders);
                return provide(cls);
            }

            //Try to log the exception, but do not recurse into X_Inject methods

            final String message = "Could not instantiate singleton for " + cls.getName();
            tryLog(message, e);
            throw new RuntimeException(message, e);
        }
    }

    protected void init(final Class<?> on, final InitMap<Class<?>, ?> map) {

        X_Log.warn(getClass(), "X_Inject encountered a class without injection metadata:", on);
        if (!"false".equals(System.getProperty("xinject.no.runtime.injection"))) {
            X_Log.info(getClass(), "Attempting runtime injection.");
            try {
                scanClasspath();
                X_Log.info(getClass(), "Runtime injection success.");
            } catch (final Exception e) {
                X_Log.warn(getClass(), "Runtime injection failure.", e);
            }
        }
    }

    private void scanClasspath() {
        runtimeInjector.get().in(
            System.getProperty(PROPERTY_RUNTIME_META, "target/classes"), checker
        );
    }

    @Override
    public <T> void setInstanceFactory(final Class<T> cls, final Provider<T> provider) {
        if (X_Runtime.isDebug()) {
            X_Log.debug("Setting instance factory for ", cls);
        }
        instanceProviders.put(cls, provider);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Our target is already erased to Object
    public <T> void setSingletonFactory(final Class<T> cls, final Provider<T> provider) {
        if (X_Runtime.isDebug()) {
            X_Log.debug("Setting singleton factory for ", cls);
        }
        singletonProviders.put(cls, (Provider) provider);
    }

    public void initialize(final Object o) {
    }

}
