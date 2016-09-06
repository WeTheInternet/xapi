package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/21/16.
 */
public interface HasMutator <Self extends Object & HasMutator<Self>> extends ReturnSelf<Self>, Cloneable, Rethrowable {

    Mutator<Self> mutate();

    interface Mutator <Self> {

        <Value, Bound extends Value> Mutable <Value> property(Class<Bound> type, String key);

        default <Value, Bound extends Value> Mutable <Value> getOrCreate(
            Class<Bound> type, String key, In1Out1<Class<Bound>, Value> factory) {
            final Mutable<Value> property = property(type, key);

            Value peek = property.out1();
            if (peek == null) {
                peek = factory.io(type);
                if (peek != null) {
                    property.in(peek);
                }
            } else if (!type.isInstance(peek)) {
                throw new ClassCastException("Value " + peek + " is not a " + type);
            }
            return property;
        }

    }

}
