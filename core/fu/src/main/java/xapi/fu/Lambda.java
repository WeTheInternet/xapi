package xapi.fu;

import java.io.Serializable;

/**
 * Marker interface with some helper methods for lambdas;
 * most notable, implementing {@link Comparable#compareTo(Object)} to net you easy equals (just check for 0).
 * It's up to you to implement Comparable, we just provide Lambda.super.compareTo() for your convenience.
 *
 * It's also up to you to implement Serializable if you want stable lambda names
 * (that is, to be able to compute a usable uuid for a given lambda instance).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public interface Lambda {

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
                int myCode = System.identityHashCode(this);
                int yourCode = System.identityHashCode(o);
                return myCode - yourCode; // in a real jvm this is random
                // in gwt, this is likely incremental, suggesting we might
                // want to fuzz this up to ensure both are random...
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
