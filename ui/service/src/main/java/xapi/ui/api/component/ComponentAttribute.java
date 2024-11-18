package xapi.ui.api.component;

import xapi.fu.In1Out1;
import xapi.fu.Mutable;
import xapi.fu.Out2;
import xapi.source.api.HasSource;

/**
 * A descriptor for a given attribute of a component.
 *
 * To make cross-component reusable attributes,
 * you may subclass this implementation,
 * otherwise, use the Builder.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/18/17.
 */
public interface ComponentAttribute <T> {

    Out2<String, Mutable<T>> asPair();

    default String getKey() {
        return asPair().out1();
    }

    default Mutable<T> getValue() {
        return asPair().out2();
    }

    default ComponentAttributeBuilder<T> mutate() {
        return new ComponentAttributeBuilder<>(this);
    }

    default String serialized() {
        final String value = getValueSerialized();
        if (value.isEmpty() && isCollapseWhenEmpty()) {
            return getKey() + " ";
        }
        return getKey() +" = " + value;
    }

    default boolean isCollapseWhenEmpty() {
        return true;
    }

    default In1Out1<T, String> getValueSerializer() {
        return value -> {
            if (value == null) {
                return "";
            }
            // do this early to encourage compiler inlining
            if (value instanceof String) {
                return (String)value;
            }
            if (value instanceof HasSource) {
                return ((HasSource)value).toSource();
            }
            return String.valueOf(value);
        };
    }

    default String getValueSerialized() {
        final T value = getValue().out1();
        final String serialized = getValueSerializer().io(value);
        return serialized;
    }

}
