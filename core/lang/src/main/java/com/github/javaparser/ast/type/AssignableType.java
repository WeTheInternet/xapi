package com.github.javaparser.ast.type;

import com.github.javaparser.ast.NamedNode;

/**
 * This is a common supertype for {@link PrimitiveType}, {@link ClassOrInterfaceType} and {@link ReferenceType}.
 *
 * A PrimitiveType contains only a name of it's type.
 * A ClassOrInterfaceType has an optional enclosing scope (also a class or interface),
 * as well as a name and optional type parameters.
 * A ReferenceType can only contain PrimitiveType or ClassOrInterfaceType, and adds array arity to a type.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/20/18 @ 12:46 AM.
 */
public interface AssignableType extends NamedNode {

    default String getNameRaw() {
        return getName();
    }
}
