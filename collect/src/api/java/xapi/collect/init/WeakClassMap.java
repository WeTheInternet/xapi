package xapi.collect.init;

import xapi.fu.In1Out1;

import java.util.WeakHashMap;

/**
 * WeakClassMap:
 * <p><p>
 *     An {@link InitMapDefault} which is backed by a synchronized WeakHashMap, using classes for keys.
 * <p><p>
 *     This lets you lazily compute some throwawable-value keyed to a Class.
 * <p><p>
 *     You can easily created alterations via {@code new InitMapDefault(..., new WeakHashMap<>())}.
 * <p><p>
 * Created by James X. Nelson (James@WeTheInter.net) on 06/05/2021 @ 12:17 a.m..
 */
public class WeakClassMap <V> extends InitMapDefault<Class, V> {
    private static final In1Out1<Class, String> CLASS_CANONCIAL = Class::getCanonicalName;

    public WeakClassMap(final In1Out1<Class, V> valueProvider) {
        super(CLASS_CANONCIAL, valueProvider, new WeakHashMap<>());
    }
}
