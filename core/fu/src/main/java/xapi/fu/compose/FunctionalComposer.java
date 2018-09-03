package xapi.fu.compose;

/**
 * This is a base interface for functional composers which can have multiple
 * operations queued up before assembling a final result which encompasses the
 * whole arrangement of operations requested.
 *
 * For instance, if you wish to make a functional object serializable,
 * but then wrap it in a non-serializable fashion,
 * the end result will not be serializable anymore.
 *
 * Using a functional composer allows you to specify these details and create
 * a functional object that both has any types / features you want,
 * and which, if further composed, will continue to retain previous settings.
 *
 * The implementations of this type will be generated, as it is simply
 * far too much boilerplate to actually wire this up for M types of N arity.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/28/17.
 */
public interface FunctionalComposer <T> {

    default boolean isSerializable() {
        return false;
    }

    default boolean isThreadsafe() {
        return false;
    }

    default boolean isUnsafe() {
        return false;
    }

    default boolean isLazy() {
        return false;
    }

    T create();

}
