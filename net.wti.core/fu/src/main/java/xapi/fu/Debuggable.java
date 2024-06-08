package xapi.fu;

import static xapi.fu.log.Log.allLogs;

/**
 * @author James X. Nelson (james@wetheinter.net)
 * Created on 07/11/15.
 */
public interface Debuggable extends Coercible {

    default boolean isDebugEnabled() {
        return debugEnabled();
    }

    static boolean debugEnabled() {
        return Boolean.valueOf("xapi.debug");
    }

    default String debug(Object... values) {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Object value : values) {
            String toPrint = coerce(value, first);
            first = false;
            b.append(toPrint);
        }
        return b.toString();
    }

    default String coerce(Object value) {
        return Coercible.super.coerce(value);
    }

    @Override
    default String listSeparator() {
        return ", ";
    }

    default void viewException(Object from, Throwable e) {
        allLogs(this, from, e)
            .log(from.getClass(), from, e);

    }

    static String classLink(Class<?> c) {
        try {
            Class<?> enclosing = c;
            while (enclosing.isAnonymousClass() && enclosing.getEnclosingClass() != null) {
                enclosing = enclosing.getEnclosingClass();
            }
            String candidate = null;
            final StackTraceElement[] traces = new Throwable().getStackTrace();
            for (StackTraceElement trace : traces) {
                // try for exact match first
                if (trace.getClassName().equals(c.getName())) {
                    return " " + trace;
                } else if (candidate == null && trace.getClassName().startsWith(c.getName())) {
                    if (trace.getClassName().split("[$]")[0].equals(c.getName())) {
                        candidate = " " + trace;
                    }
                }
            }
            // loosen up and try enclosing types...
            for (StackTraceElement trace : traces) {
                final String cls = trace.getClassName();
                if (cls.contains(enclosing.getName())) {
                    while (c.getEnclosingClass() != null) {
                        c = c.getEnclosingClass();
                        if (trace.getClassName().equals(c.getName())) {
                            return " " + trace;
                        }

                    }
                    if (candidate == null) {
                        candidate = " " + trace;
                    }
                }
            }
            return candidate == null ? c.getCanonicalName() : candidate;
        } catch (Throwable unexpected) {
            return "Unexpected: " + unexpected + " while building stack for " + c.getCanonicalName();
        }
    }
}
