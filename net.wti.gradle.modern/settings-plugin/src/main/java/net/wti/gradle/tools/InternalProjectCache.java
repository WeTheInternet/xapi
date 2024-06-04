package net.wti.gradle.tools;

import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.System.identityHashCode;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/23/19 @ 1:16 AM.
 */
public final class InternalProjectCache <T extends ExtensionAware> {
    public static final String EXT_NAME = "_xapiInternal";
    private final T from;
    private ExtensionContainer privates;

    public InternalProjectCache(T ext) {
        this.from = ext;
    }

    public InternalProjectCache(T ext, Object foreignCopy) {
        this.from = ext;
        if (foreignCopy != null) {
            try {
                final Field field = foreignCopy.getClass().getDeclaredField("privates");
                field.setAccessible(true);
                privates = (ExtensionContainer) foreignCopy.getClass().getDeclaredField("privates").get(foreignCopy);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to reflectively copy privates from foreign copy", e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("No privates field found in " + foreignCopy +" (type " + foreignCopy.getClass() + ")");
            }
        }
    }

    public boolean isFrom(ExtensionAware source) {
        return from.getExtensions() == source.getExtensions();
    }

    public <O> O getOrCreate(
            Class<? super O> publicType,
            T from,
            String key,
            BiFunction<? super T, String, ? extends O> factory
    ) {
        final ExtensionContainer extensions = from.getExtensions();
        if (publicType != null) {
            // avoid class cast issues (TODO: profile for leaked memory; we know projects w/ different classloaders can't nicely share state...
            key = key + System.identityHashCode(publicType);
        }
        final Object val = extensions.findByName(key);
        if (val == null) {
            final O created = factory.apply(from, key);
            // In case the user code we just called already installed the object on our ExtensionAware, we don't want to double-add...
            if (null == extensions.findByName(key)) {
                // user didn't add object to the extension when we called factory.apply, do it for them
                if (publicType == null) {
                    extensions.add(key, created);
                } else {
                    extensions.add(publicType, key, created);
                }
            }
            return created;
        }
        if (publicType != null) {
            if (!publicType.isAssignableFrom(val.getClass())) {
                throw new IllegalStateException("The extension " + val + " uses key " + key + " but is not assignable to " + publicType);
            }
        }
        return (O) val;
    }

    public static <T extends ExtensionAware> void doOnce(T ext, String key, Action<? super T> callback) {
        if (!Boolean.TRUE.equals(ext.getExtensions().findByName(key))) {
            ext.getExtensions().add(key, true);
            callback.execute(ext);
        }
    }

    public static <T extends ExtensionAware, O> O buildOnce(T ext, String key, Function<? super T, ? extends O> factory) {
        return buildOnce(null, ext, key, factory);
    }

    public static <T extends ExtensionAware, O> O buildOnce(Class<? super O> publicType, T ext, String key, Function<? super T, ? extends O> factory) {
        return buildOnce(publicType, ext, key, (p, k)->factory.apply(p));
    }

    public static <T extends ExtensionAware, O> O buildOnce(T ext, String key, BiFunction<? super T, String, ? extends O> factory) {
        return buildOnce(null, ext, key, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExtensionAware, O> O buildOnce(Class<? super O> publicType, T source, String key, BiFunction<? super T, String, ? extends O> factory) {
        InternalProjectCache<T> cache = getCache(source);
        return cache.getOrCreate(publicType, source, key, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExtensionAware> InternalProjectCache<T> getCache(T ext) {
        final ExtensionContainer extensions = ext.getExtensions();
        InternalProjectCache<T> cache;
        final String cacheSuffix = Integer.toString(identityHashCode(InternalProjectCache.class));
        String key = InternalProjectCache.EXT_NAME + cacheSuffix;
        try {
            cache = (InternalProjectCache) extensions.findByName(key);
        } catch (ClassCastException cce) {
            Object o = extensions.findByName(key);
            try {
                cache = new InternalProjectCache<>(ext, o);
            } catch (Exception ignored) {
                // give up. stomp old cache (debugging shows this happens to be buildSrc leaving a cache on root project somehow)
                cache = new InternalProjectCache<>(ext);
            }
        }
        boolean empty = cache == null;
        boolean incompatible = !empty && !cache.isFrom(ext);
        if (empty) {
            cache = new InternalProjectCache<>(ext);
            extensions.add(key, cache);
        } else if (incompatible) {
            // hmmm... try to throw away / cleanup the old cache? yuck...... we don't really have a good line on a logger either
            cache = new InternalProjectCache<>(ext);
            extensions.add(key, cache);
        }
        return cache;
    }
}
