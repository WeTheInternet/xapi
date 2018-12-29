package net.wti.gradle.system.tools;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.Named;
import org.gradle.api.provider.Provider;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:11 PM.
 */
public class GradleCoerce {

    private static final Function<Object, Object> DEFAULT_FALLBACK = item->{
        try {
            final Method method = item.getClass().getMethod("out1");
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

    public static String unwrapString(Object o) {
        final List<String> unwrapped = unwrapStrings(o);
        assert unwrapped.size() < 2 : "Got more than one string unwrapping " + o + " : " + unwrapped;
        return unwrapped.isEmpty() ? null : unwrapped.get(0);
    }
    public static List<String> unwrapStrings(Object o) {
        final List<Object> items = unwrap(o, item -> {
            if (item instanceof Named) {
                return ((Named) item).getName();
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
                result.addAll(unwrap(item));
            }
        } else if (o instanceof Provider) {
            final Object item = ((Provider) o).get();
            result.addAll(unwrap(item));
        } else if (o instanceof Closure) {
            final Object item = ((Closure) o).call();
            result.addAll(unwrap(item));
        } else if (o instanceof Supplier) {
            final Object item = ((Supplier) o).get();
            result.addAll(unwrap(item));
        } else if (o.getClass().isArray()) {
            for (int i = 0, m = Array.getLength(o); i < m; i++) {
                final Object item = Array.get(o, i);
                result.addAll(unwrap(item));
            }
        } else {
            final Object item = fallback.apply(o);
            if (item == o) {
                throw failure(o);
            }
            result.addAll(unwrap(item));
        }
        return result;
    }

    private static RuntimeException failure(Object o) {
        return new UnsupportedOperationException("Cannot coerce " + o.getClass() + ": " + o);
    }
}
