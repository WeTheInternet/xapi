package xapi.source.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/16/17.
 */
@SuppressWarnings("all") // gwt plugin complains because our sdk is better :D
public class TypeBuilder {

    public static class ImmutableTypeVariable<D extends GenericDeclaration> implements TypeVariable<D> {

        @Override
        public Type[] getBounds() {
            return new Type[0];
        }

        @Override
        public D getGenericDeclaration() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return new AnnotatedType[0];
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }
    }

    public static class ImmutableGenericArrayType implements GenericArrayType {

        private final Type componentType;

        public ImmutableGenericArrayType(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }
    }

    public static class ImmutableParameterizedType implements ParameterizedType {

        private final Type rawType;
        private final Type ownerType;
        private final Type[] actualTypeArguments;

        public ImmutableParameterizedType(Type rawType, Type ownerType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            this.ownerType = ownerType;
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }
    }

    public static class ImmutableWildcard implements WildcardType {

        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        public ImmutableWildcard(Type[] upperBounds) {
            this.upperBounds = upperBounds;
            this.lowerBounds = null;
        }

        public ImmutableWildcard(boolean lower, Type[] bounds) {
            if (lower) {
                this.upperBounds = null;
                this.lowerBounds = bounds;
            } else {
                this.upperBounds = bounds;
                this.lowerBounds = null;
            }
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds;
        }
    }


}
