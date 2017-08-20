package xapi.ui.api.component;

import xapi.fu.Mutable;
import xapi.fu.Out2;
import xapi.fu.Out2.Out2Immutable;

import static xapi.fu.Out2.out2Immutable;

/**
 * A builder for a component attribute.
 *
 * Used to declaratively define some arbitrary value
 * that you want to assign to a string-keyed attribute.
 *
 * All implementations must provide a serializer,
 * so your attributes can be flattened,
 * at the very least for debugging purposes.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/18/17.
 */
public class ComponentAttributeBuilder <T> {

    /**
     * We use Mutable<T> because we want a final reference to a source variable;
     * we will supply immutable copies which read from shared, original source.
     *
     * This allows many instances of a builder to share a reference,
     * so that if one of them is updated, all of them are updated.
     */
    private final Mutable<T> value;
    private String key;
    private boolean shareValue;

    public ComponentAttributeBuilder(ComponentAttribute<T> source) {
        this.key = source.getKey();
        this.value = source.getValue();
    }

    public ComponentAttribute<T> build() {
        final Out2<String, Mutable<T>> pair;
        if (isShareValue()) {
            pair = out2Immutable(key, value);
        } else {
            // the default behavior it to give a local copy...
            pair = out2Immutable(key, value.copy());
        }
        return ()->pair;
    }

    public Mutable<T> getValue() {
        return value;
    }

    public final String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public final boolean isShareValue() {
        return shareValue;
    }

    public void setShareValue(boolean shareValue) {
        this.shareValue = shareValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final ComponentAttributeBuilder<?> that = (ComponentAttributeBuilder<?>) o;

        if (shareValue != that.shareValue)
            return false;
        if (value != null ? !value.equals(that.value) : that.value != null)
            return false;
        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (shareValue ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ComponentAttributeBuilder{" +
            "value=" + value +
            ", key='" + key + '\'' +
            ", shareValue=" + shareValue +
            '}';
    }
}
