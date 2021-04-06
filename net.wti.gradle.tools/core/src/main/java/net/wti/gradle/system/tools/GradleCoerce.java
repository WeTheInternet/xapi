package net.wti.gradle.system.tools;

import org.gradle.api.GradleException;
import org.gradle.api.Named;
import org.gradle.api.provider.Provider;
import org.gradle.util.GUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:11 PM.
 */
public class GradleCoerce {

    private static final Function<Object, Object> DEFAULT_FALLBACK = item->{
        try {
            Method method;
            try {
                // prefer xapi types (Out1.out())
                method = item.getClass().getMethod("out1");
            } catch (NoSuchMethodException e) {
                try {
                    // next, plain get()
                    method = item.getClass().getMethod("get");
                } catch (NoSuchMethodException ee) {
                    // then, call() w/ no arguments (Callable)
                    method = item.getClass().getMethod("call");
                }
            }
            item = method.invoke(item);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Found method out1(), but it was not visible", e);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Cannot coerce " + item.getClass() + ": " + item);
        } catch (InvocationTargetException e) {
            throw new GradleException("Unexpected exception calling " + item, e);
        }
        return item;
    };

    public static String unwrapStringOr(Object o, String dflt) {
        return GUtil.elvis(unwrapString(o), dflt);
    }

    public static String unwrapString(Object o) {
        return unwrapString(o, true);
    }

    public static <T extends Enum<T>> T unwrapEnum(Class<T> cls, Object o) {
        String v = unwrapString(o, true);
        return Enum.valueOf(cls, v);
    }

    @Nonnull
    public static String unwrapStringNonNull(Object o) {
        final String result = unwrapString(o, false);
        assert result != null : "nullOnEmpty:false failed when resolving " + o;
        return result;
    }

    public static String unwrapString(Object o, boolean nullOnEmpty) {
        if (o instanceof CharSequence) {
            return o.toString();
        }
        final List<String> unwrapped = unwrapStrings(o);
        assert unwrapped.size() < 2 : "Got more than one string unwrapping " + o + " : " + unwrapped;
        assert unwrapped.isEmpty() || unwrapped.get(0) != null : "Do not supply null strings!";
        return unwrapped.isEmpty() ? nullOnEmpty ? null : "" : unwrapped.get(0);
    }

    public static List<String> unwrapStrings(Object o) {
        final List<Object> items = unwrap(o, item -> {
            if (item instanceof Named) {
                return ((Named) item).getName();
            }
            if (item instanceof CharSequence
                || item instanceof File
                || item instanceof Number
                || item instanceof Boolean) {
                return item.toString();
            }
            if (item instanceof Enum) {
                return ((Enum) item).name();
            }
            return DEFAULT_FALLBACK.apply(o);
        });
        return items.stream().map(String::valueOf).collect(Collectors.toList());
    }

    public static List<Object> unwrap(Object o) {
        return unwrap(o, DEFAULT_FALLBACK);
    }

    public static List<Object> unwrap(Object o, Function<Object, Object> fallback) {
        final List<Object> result = new ArrayList<>();
        if (o instanceof Iterable) {
            for (Object item : ((Iterable) o)) {
                result.addAll(unwrap(item, fallback));
            }
        } else if (o instanceof Provider) {
            final Object item = ((Provider) o).get();
            result.addAll(unwrap(item, fallback));
        } else if (o instanceof Callable) {
            final Object item;
            try {
                item = ((Callable) o).call();
                result.addAll(unwrap(item, fallback));
            } catch (Exception e) {
                throw new GradleException("Unexpected exception invoking " + o, e);
            }
        } else if (o == null) {
            return result;
        } else if (o instanceof Supplier) {
            final Object item = ((Supplier) o).get();
            result.addAll(unwrap(item, fallback));
        } else if (o.getClass().isArray()) {
            for (int i = 0, m = Array.getLength(o); i < m; i++) {
                final Object item = Array.get(o, i);
                result.addAll(unwrap(item, fallback));
            }
        } else {
            final Object item = fallback.apply(o);
            if (item == o) {
                result.add(item);
            } else {
                result.addAll(unwrap(item, fallback));
            }
        }
        return result;
    }

    private static RuntimeException failure(Object o) {
        return new UnsupportedOperationException("Cannot coerce " + o.getClass() + ": " + o);
    }

    public static boolean unwrapBoolean(Object sourceAllowed) {
        switch (unwrapString(sourceAllowed)) {
            case "true":
            case "TRUE":
            case "True":
            case "1":
                return true;
        }
        return false;
    }

    public static boolean isEmpty(Object value) {
        return unwrapStringNonNull(value).isEmpty();
    }

    public static boolean isEmptyString(CharSequence value) {
        return value == null || value.length() == 0;
    }

    public static String toTitleCase(String name) {
        return isEmpty(name) ? name : Character.toUpperCase(name.charAt(0))+
                (name.length() == 1 ? "" : name.substring(1));
    }

}
