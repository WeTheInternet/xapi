package xapi.collect.init;

import xapi.fu.In1Out1;

import java.util.WeakHashMap;

///
/// WeakClassMap:
///
///     An {@link InitMapDefault} which is backed by a synchronized WeakHashMap, using classes for keys.
///
///     This lets you lazily compute some throwawable-value keyed to a Class.
///
///     You can easily create alterations via `new InitMapDefault(..., new WeakHashMap<>())`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 06/05/2021 @ 12:17 a.m.
///
public class WeakClassMap <V> extends InitMapDefault<Class, V> {
    private static final In1Out1<Class, String> CLASS_CANONCIAL = Class::getCanonicalName;

    public WeakClassMap(final In1Out1<Class, V> valueProvider) {
        super(CLASS_CANONCIAL, valueProvider, new WeakHashMap<>());
    }
}
