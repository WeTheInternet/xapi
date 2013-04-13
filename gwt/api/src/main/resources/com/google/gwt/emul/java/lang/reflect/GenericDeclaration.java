package java.lang.reflect;

import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.TypeVariable;

/**
 * A common interface for all entities that declare type variables.
 *
 * @since 1.5
 */
public interface GenericDeclaration {
    /**
     * Returns an array of <tt>TypeVariable</tt> objects that
     * represent the type variables declared by the generic
     * declaration represented by this <tt>GenericDeclaration</tt>
     * object, in declaration order.  Returns an array of length 0 if
     * the underlying generic declaration declares no type variables.
     *
     * @return an array of <tt>TypeVariable</tt> objects that represent
     *     the type variables declared by this generic declaration
     * @throws GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in the Java Virtual Machine Specification,
     *     3rd edition
     */
    public TypeVariable<?>[] getTypeParameters();
}