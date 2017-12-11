package xapi.fu.has;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/12/17.
 */
public interface HasEmptiness {

    static boolean isEmpty(HasEmptiness item) {
        return item == null || item.isEmpty();
    }

    boolean isEmpty();

    default boolean isNotEmpty() {
        return !isEmpty();
    }
}
