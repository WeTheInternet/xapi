package net.wti.gradle.test.api;

import org.gradle.api.Action;

import java.io.File;
import java.util.Map;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 4:17 AM.
 */
public interface HasProjectFiles extends HasTestFiles {

    default <K, V> Action<? super V> insert(Map<K, Action<? super V>> into, K name, Action<? super V> a) {
        return into.compute(name, (key, val)->
            val == null ? a : p -> { val.execute(p); a.execute(p); }
        );
    }

    File getBuildFile();

    File getPropertiesFile();

}
