package net.wti.gradle.system.api;

import java.util.concurrent.Callable;

/**
 * An efficient {@link Callable} wrapper which implements toString(),
 * as well as call-only-once semantics on potentially expensive operations.
 *
 * java:
 * LazyString later = LazyString.nullableString(this::generateExpensiveString);
 * assert later.call() == later.toString();
 * assert generateExpensiveStringOnlyCalledOnce();
 *
 * groovy:
 * LazyString later = LazyString.nonNullString({"${charSequences} are acceptable $input"})
 *
 * We also defer hashCode(), equals() and compareTo() to the underlying callable,
 * so you can safely use these as keys of a map, in place of strings.
 *
 * This object uses an efficient and concise "run once, and do not lock afterwards",
 * without needing volatile fields or expensive mutex objects.
 *
 * If your supplied callable returns null, it will be called until it does not return null.
 * Your supplied Callable will be discarded after it is used (returns non-null).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/31/19 @ 11:45 PM.
 */
public class LazyString implements Callable<String>, Comparable<LazyString> {

    private Callable<String> source;

    public LazyString(Callable<? extends CharSequence> supplier) {
        this(supplier, false);
    }

    public LazyString(Callable<? extends CharSequence> supplier, boolean nonNull) {
        final String[] value = {null};
        this.source = ()->{
            synchronized (value) {
                if (value[0] == null) {
                    final CharSequence result = supplier.call();
                    if (result != null) {
                        value[0] = result.toString();
                        // short-circuit for future callers to skip synchro.
                        source = new Immutable<>(value[0]);
                    } else if (nonNull) {
                        throw new IllegalStateException("Illegal null received from " + supplier);
                    }
                }
            }
            return value[0];
        };
    }

    @Override
    public String call() {
        try {
            return source.call();
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException)e :
                  new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return call();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        // allow subclassing to work
        if (o == null || !LazyString.class.isAssignableFrom(o.getClass()))
            return false;

        final LazyString that = (LazyString) o;

        return call().equals(that.call());
    }

    @Override
    public int hashCode() {
        return call().hashCode();
    }

    @Override
    public int compareTo(LazyString o) {
        return call().compareTo(o.call());
    }

    public static LazyString nullableString(Callable<? extends CharSequence> value) {
        return new LazyString(value, false);
    }

    public static LazyString nonNullString(Callable<? extends CharSequence> value) {
        return new LazyString(value, true);
    }
}
