package xapi.fu.api;

import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.X_Fu;

/**
 * "Marker interface" with helper methods for unsafe variants of other functional utility types
 * (In, Out, InOut, Maybe, etc).  Instances of these types may freely throw any exception they like.
 *
 * If additional error handling is done by the client, your exception will not be rewrapped and thrown.
 * If you do not specify what to do with errors, they will be rethrown (as one would expect if you were not
 * aware of this feature).
 *
 *
 * @param <R> - A return type for your unsafe operation.
 *           If your operation does not return synchronously,
 *           you should use Void as your type, and null as your value.
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/28/17.
 */
public interface IsUnsafe <R> {

    default R doUnsafe(Out1Unsafe<R> task) {
        try {
            R value = task.outUnsafe();
            return value;
        } catch (Throwable throwable) {
            errorHandler().in(throwable);
            return null;
        }
    }

    default In1<Throwable> errorHandler() {
        return X_Fu::rethrow;
    }

}
