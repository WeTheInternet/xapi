package xapi.fu;

import java.io.Serializable;

/**
 * Marker interface with some helper methods for lambdas;
 * most notable, implementing {@link Comparable} to net you easy equals (just check for 0)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface Lambda extends Serializable { // we want to force usable names...

    // we can't override Object methods, but we can do Comparable interface method
    // Note we don't explicitly extend Comparable, to let a lambda type decide if it's comparable.
    default int compareTo(Lambda o) {
        if (o == this) {
            return 0;
        }
        // when comparing instances
        String myName = X_Fu.getLambdaMethodName(this);
        String yourName = X_Fu.getLambdaMethodName(o);
        if (myName == null) {
            if (yourName == null) {
                return 0;
            }
            return 1; // put nulls at the end
        }
        if (yourName == null) {
            // put non-nulls at the beginning
            return -1;
        } else {
            return myName.compareTo(yourName);
        }
    }
}
