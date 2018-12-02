package xapi.fu.inject;

import xapi.fu.Out1;
import xapi.fu.api.ShouldOverride;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;
import xapi.fu.log.LogInjector;

import java.io.InputStream;
import java.net.URL;

/**
 * An interface and set of helper methods for injecting types.
 *
 * You should not inherit this on the types you want to inject;
 * instead, you should create a {@code MyTypeInjector implements Injectable<MyType>}.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/20/18 @ 10:09 PM.
 */
public interface Injectable <T> {

    T createInstance(Object ... args);

    @ShouldOverride // classes should override this, lambdas don't care (only classes loaded by name call this)
    default Class<? extends T> injectionType() {
        // yo dawg, I heard you like class...
        return Class.class.cast(Class.class);
    }

    @SuppressWarnings("unchecked")
    static <T, Bound extends T> T injectStatic(final Class<Bound> type, final Out1<T> backup) {
        Object t = InjectCache.statics.getOrCreateFrom(type, ()->
            performInject(type, type.getClassLoader(), "META-INF/singletons", backup)
        );
        assert type.isInstance(t) : "Not a " + type + " : " + t;
        return (T) t;
    }
    static <T, Bound extends T> T injectInstance(final Class<Bound> type, final Out1<T> backup) {
        return performInject(type, type.getClassLoader(), "META-INF/instances", backup);
    }
    @SuppressWarnings({"unchecked"}) // we actually checked
    static <T, Bound extends T> T performInject(final Class<Bound> type, ClassLoader cl, String path, final Out1<T> backup) {
        // Hm... should we return Injectable<T>, and take a boolean whether to cache instances or not?
        if (!path.isEmpty()) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!path.endsWith("/")) {
                path = path + "/";
            }
        }
        final String target = path + type.getName();
        if (cl == null) {
            cl = type.getClassLoader();
        }
        final URL choice = cl.getResource(target);
        if (choice == null) {
            return backup.out1();
        }
        Object alt = null;
        try {
            StringBuilder b = new StringBuilder();
            byte[] config = new byte[4096]; // ridiculously oversized for one classname
            int amt;
            try (InputStream in = choice.openStream()) {
                while ((amt = in.read(config)) != -1) {
                    b.append(new String(config, 0, amt));
                }
            }
            // got the filename.  new it up.
            final Class<?> clz = cl.loadClass(b.toString().trim());
            final Object result = alt = clz.newInstance();
            if (type.isInstance(result)) {
                return (T) result;
            }
            String msg;
            if (result instanceof Injectable) {
                // Check the type parameter... if you don't override injectionType(), we won't inject you
                final Class injectType = ((Injectable) result).injectionType();
                if (type.isAssignableFrom(injectType)) {
                    final Object inst = alt = ((Injectable) result).createInstance(cl);
                    if (type.isInstance(inst)) {
                        return (T) inst;
                    } else {
                        assert false : "Injection of " + b + " returned a " + result.getClass() + " which does not extend " + type + ", " +
                            "and implements Injectable, but returned a new instance of " + inst.getClass() + " that" + " is not instanceof " + type;
                        msg = inst.getClass() + " created by Injectable " + result.getClass() + " is not instanceof " + type;
                    }
                } else if (injectType == Class.class){
                    msg = b + " must override injectionType()";
                    assert false : msg;
                } else {
                    assert false : b + " cannot resolve " + type + " as injectionType() returns " + injectType + "; which is not " +
                        "assignable to " + type;
                    msg = "Injectable " + b + " injectionType " + injectType + " is not assignable to " + type;
                }
            } else {
                assert false : "Injection of " + b + " returned a " + result.getClass() + " which does not extend " + type + ", " +
                    "or implement Injectable<" + type + ">";
                msg = b + " must extend " + type + " or implement Injectable<" + type + ">";
            }
            Log log;
            final T fallback = backup.out1();
            if (type.getName().equals(LogInjector.class.getName())) {
                // Special treatment for LogInjector, since it calls into here to get created,
                // it wouldn't due to try Log.firstLog, as it would call right back into here.
                LogInjector injector;
                try {
                    injector = (LogInjector) fallback;
                } catch (Exception e) {
                    injector = new LogInjector();
                }
                log = injector.defaultLogger();
            } else {
                log = Log.firstLog(fallback, result, backup, cl);
            }
            log.log(Injectable.class, LogLevel.ERROR, "Bad injection for ", type, msg);

            return fallback;
        } catch (Throwable e) {
            final T result = backup.out1();
            Log log;
            if (result instanceof Log) {
                log = (Log) result;
            } else if (alt instanceof Log) {
                log = (Log) alt;
            } else if (backup instanceof Log) {
                log = (Log) backup;
            } else if (cl instanceof Log) {
                log = (Log)cl;
            } else if (LogInjector.class.getName().equals(type.getName())){
                // The only form of recovery here...
                final LogInjector logger = (LogInjector) result;
                logger.defaultLogger()
                    .log(LogInjector.class, LogLevel.ERROR, "Error injecting ", type, e);
                return (T)logger;
            } else {
                log = new LogInjector().defaultLogger();
            }

            log.log(LogInjector.class, LogLevel.ERROR, "Error injecting ", type, e);

            if (e instanceof Error) {
                // after we've logged it, keep throwing.
                throw (Error)e;
            }
            return backup.out1();
        }
    }

}

class InjectCache {
    static final MapLike<Class<?>, Object> statics;
    static final MapLike<Class<?>, Out1> instances;
    static {
        MapLike map;
        try {
            map = Injectable.performInject(MapLike.class, InjectCache.class.getClassLoader(),
                "META-INF/singletons", X_Jdk::mapHashConcurrent);
        } catch (Exception e) {
            e.printStackTrace();
            // must not fail here.
            map = X_Jdk.mapHashConcurrent();
        }
        statics = map; // TODO map.threadsafe() which either returns `this` if threadsafe, or a threadsafe copy of some kind.
        try {
            map = Injectable.performInject(MapLike.class, InjectCache.class.getClassLoader(),
                "META-INF/instances", X_Jdk::mapHashConcurrent);
        } catch (Exception e) {
            e.printStackTrace();
            // must not fail here.
            map = X_Jdk.mapHashConcurrent();
        }
        instances = map;
    }
}
