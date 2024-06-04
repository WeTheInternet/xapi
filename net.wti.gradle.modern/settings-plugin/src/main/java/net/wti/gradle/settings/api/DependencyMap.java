package net.wti.gradle.settings.api;

import java.util.EnumMap;
import java.util.Map;

/**
 * DependencyMap:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 03/06/2024 @ 2:01 a.m.
 */
public class DependencyMap <V> extends EnumMap<DependencyKey, V> {

    public DependencyMap() {
        super(DependencyKey.class);
    }

    public DependencyMap(EnumMap<DependencyKey, ? extends V> m) {
        super(m);
    }

    public DependencyMap(Map<DependencyKey, ? extends V> m) {
        super(m);
    }
}

