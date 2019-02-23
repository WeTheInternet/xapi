package net.wti.gradle.internal.system;

import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.extensibility.DefaultConvention;

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
        final Object val = extensions.findByName(key);
        if (val == null) {
            final O created = factory.apply(from, key);
            if (null == extensions.findByName(key)) {
                // In case the user code already installed the object, we don't want to double-add...
                if (publicType == null) {
                    extensions.add(key, created);
                } else {
                    extensions.add(publicType, key, created);
                }
            }
            return created;
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
