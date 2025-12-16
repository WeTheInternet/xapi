/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package net.wti.lang.parser.ast.type;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.TypeArguments;
import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Julio Vilmar Gesser
 */
public final class ClassOrInterfaceType extends Type implements AssignableType {

    private ClassOrInterfaceType scope;

    private String name;

    private TypeArguments typeArguments = TypeArguments.EMPTY;

    public ClassOrInterfaceType() {
    }

    public ClassOrInterfaceType(final String name) {
        int generics = name.indexOf('<');
        if (generics == -1) {
            setName(name);
        } else {
            setName(name.substring(0, generics));
            try {
                Type parsed = JavaParser.parseType(name);
                if (parsed instanceof ReferenceType) {
                    assert ((ReferenceType)parsed).getArrayCount() == 0;
                    parsed = ((ReferenceType) parsed).getType();
                }
                if (parsed instanceof ClassOrInterfaceType) {
                    final TypeArguments typeArgs = ((ClassOrInterfaceType) parsed).getTypeArguments();
                    final List<Type> types = typeArgs.getTypeArguments();
                    for (int i = 0; i < types.size(); i++) {
                        Type type = types.get(i);
                        String simple = type.toSource();
                        final String qualified = WellKnownTypes.qualifyType(simple);
                        if (!qualified.equals(simple)) {
                            type = JavaParser.parseType(qualified);
                            types.set(i, type);
                        }
                    }
                    setTypeArguments(typeArgs);
                } else {
                    throw new IllegalStateException("Cannot extract generics from " + name +" ; got: " + parsed.toSource());
                }
            } catch (ParseException e) {
                throw new RuntimeException("Cannot parse type from " + name, e);
            }
        }
    }

    public ClassOrInterfaceType(final ClassOrInterfaceType scope, final String name) {
        setScope(scope);
        setName(name);
    }

    /**
     *
     * @deprecated use the other constructor that takes {@link TypeArguments}
     */
    @Deprecated
    public ClassOrInterfaceType(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                                final ClassOrInterfaceType scope, final String name, final List<Type> typeArgs) {
        this(beginLine, beginColumn, endLine, endColumn, scope, name, TypeArguments.withArguments(typeArgs));
    }

    public ClassOrInterfaceType(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                                final ClassOrInterfaceType scope, final String name, final TypeArguments typeArguments) {
        super(beginLine, beginColumn, endLine, endColumn);
        setScope(scope);
        setName(name);
        setTypeArguments(typeArguments);
    }

    @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getEnclosedName() {
        if (getScope() == null) {
            return name;
        }
        return getScope().getEnclosedName() +"." + name;
    }

    public ClassOrInterfaceType getScope() {
        return scope;
    }

    public List<Type> getTypeArgs() {
        return typeArguments.getTypeArguments();
    }

    public TypeArguments getTypeArguments() {
        return typeArguments;
    }

    public boolean isUsingDiamondOperator() {
        return typeArguments.isUsingDiamondOperator();
    }

    public boolean isBoxedType() {
        return PrimitiveType.unboxMap.containsKey(name);
    }

    public PrimitiveType toUnboxedType() throws UnsupportedOperationException {
        if (!isBoxedType()) {
            throw new UnsupportedOperationException(name + " isn't a boxed type.");
        }
        return new PrimitiveType(PrimitiveType.unboxMap.get(name));
    }

    public void setName(final String name) {
        int end = name.lastIndexOf('.');
        if (end == -1) {
            this.name = name;
        } else {
            this.name = name.substring(end+1);
            assert scope == null : "Cannot send both a scope and a qualified name... (you sent " + name + " and " + scope + ")";
            setScope(new ClassOrInterfaceType(name.substring(0, end)));
        }
    }

    public void setScope(final ClassOrInterfaceType scope) {
        this.scope = scope;
        setAsParentNodeOf(this.scope);
    }

    /**
     * Allows you to set the generic arguments
     * @param typeArgs The list of types of the generics
     */
    public void setTypeArgs(final List<Type> typeArgs) {
        setTypeArguments(TypeArguments.withArguments(typeArgs));
    }

    public void setTypeArguments(TypeArguments typeArguments) {
        this.typeArguments = typeArguments;
        setAsParentNodeOf(this.typeArguments.getTypeArguments());
    }

    @Override
    public boolean hasRawType(String name) {
        return this.name.equals(name);
    }

    public ClassOrInterfaceType addTypeArgs(Type ... types) {
        if (typeArguments == TypeArguments.EMPTY) {
            typeArguments = TypeArguments.withArguments(types);
        } else {
            // We need to merge our type args.
            final List<Type> existing = typeArguments.getTypeArguments();
            final ArrayList<Type> newList = new ArrayList<>(existing);
            for (Type type : types) {
                newList.add(type);
            }
            typeArguments = TypeArguments.withArguments(newList);
        }
        return this;
    }

    public boolean hasTypeArguments() {
        return typeArguments != null
            && typeArguments.getTypeArguments() != null
            && !typeArguments.getTypeArguments().isEmpty();
    }
}
