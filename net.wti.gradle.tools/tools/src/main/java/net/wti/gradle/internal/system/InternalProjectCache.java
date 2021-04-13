package net.wti.gradle.internal.system;

import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.extensibility.DefaultConvention;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

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

    public <O> O buildOnce(
        Class<? super O> publicType,
        T from,
        String key,
        BiFunction<? super T, String, ? extends O> factory
    ) {
        final ExtensionContainer extensions = publicType == null || publicType.getAnnotation(InternalExtension.class) == null ? from.getExtensions() : privateExtension();
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

    private ExtensionContainer privateExtension() {
        if (privates == null) {
            // private extensions will not be used to create objects; we'll use the original for that.
            privates = new DefaultConvention(null);
        }
        return privates;
    }
}
