package xapi.util.api;

import xapi.error.NotImplemented;
import xapi.fu.In2Out1;
import xapi.fu.X_Fu;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public class SerializableWrapper<T extends Serializable> implements Serializable {

    private final T id;
    private final int hash;
    private final In2Out1<T, T, Boolean> filter;

    public SerializableWrapper(T id, int hash, In2Out1<T, T, Boolean> filter) {
        this.id = id;
        this.hash = hash;
        this.filter = filter;
    }

    public T getValue() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SerializableWrapper) {
            obj = ((SerializableWrapper) obj).id;
            if (!id.getClass().isAssignableFrom(obj.getClass())) {
                return false;
            }
            return filter.io(id, (T) obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public static Serializable serializableId(Serializable id) {
        if (id == null) {
            return "null";
        }
        final Class<? extends Serializable> cls = id.getClass();
        if (!cls.isArray()) {
            if (X_Fu.isLambda(id)) {
                String name = X_Fu.getLambdaMethodName(id);
                if (name != null) {
                    return name;
                }
            }
            return id;
        }

        if (!cls.getComponentType().isPrimitive()) {
            // an array of arrays...
            return new SerializableWrapper<>((Object[]) id, Arrays.deepHashCode((Object[]) id), Arrays::deepEquals);
        }
        if (id instanceof int[]) {
            return new SerializableWrapper<>((int[]) id, Arrays.hashCode((int[]) id), Arrays::equals);
        } else if (id instanceof double[]) {
            return new SerializableWrapper<>((double[]) id, Arrays.hashCode((double[]) id), Arrays::equals);
        } else if (id instanceof boolean[]) {
            return new SerializableWrapper<>((boolean[]) id, Arrays.hashCode((boolean[]) id), Arrays::equals);
        } else if (id instanceof byte[]) {
            return new SerializableWrapper<>((byte[]) id, Arrays.hashCode((byte[]) id), Arrays::equals);
        } else if (id instanceof char[]) {
            return new SerializableWrapper<>((char[]) id, Arrays.hashCode((char[]) id), Arrays::equals);
        } else if (id instanceof float[]) {
            return new SerializableWrapper<>((float[]) id, Arrays.hashCode((float[]) id), Arrays::equals);
        } else if (id instanceof long[]) {
            return new SerializableWrapper<>((long[]) id, Arrays.hashCode((long[]) id), Arrays::equals);
        } else if (id instanceof short[]) {
            return new SerializableWrapper<>((short[]) id, Arrays.hashCode((short[]) id), Arrays::equals);
        } else {
            throw new NotImplemented("Cannot create a serializable wrapper for " + id);
        }
    }
}
