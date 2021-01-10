package net.wti.gradle.require.api;

import java.util.EnumMap;
import java.util.Map;

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
