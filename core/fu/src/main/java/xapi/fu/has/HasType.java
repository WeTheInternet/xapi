package xapi.fu.has;

import xapi.fu.iterate.ArrayIterable;

import java.lang.reflect.TypeVariable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
public interface HasType {

    Class<?> getType();

    default Iterable<HasType> getTypeParams() {
        final Class type = getType();
        final TypeVariable<Class<?>>[] params = type.getTypeParameters();

        return new ArrayIterable<>(params)
                    .map(this::toType);
    }

    default HasType toType(TypeVariable<Class<?>> var) {
        return var::getGenericDeclaration;
    }
}
