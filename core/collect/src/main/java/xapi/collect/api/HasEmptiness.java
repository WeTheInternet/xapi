package xapi.collect.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/12/17.
 */
public interface HasEmptiness {
    boolean isEmpty();

    default boolean isNotEmpty() {
        return !isEmpty();
    }
}
