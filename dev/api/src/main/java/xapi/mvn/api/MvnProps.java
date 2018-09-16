package xapi.mvn.api;

import xapi.fu.data.MapLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface MvnProps {

    MapLike<String, String> map();

    default String normalize(String key) {
        if (key.startsWith("$")) {
            return key.substring(2, key.length() - 1);
        }
        return key;
    }

    default boolean hasProperty(String key) {
        return map().has(normalize(key));
    }

    default String getProperty(String key) {
        return map().get(normalize(key));
    }

    default String setProperty(String key, String value) {
        return map().get(normalize(key));
    }

}
