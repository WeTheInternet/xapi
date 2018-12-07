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

package com.github.javaparser.ast.expr;

import xapi.fu.itr.MappedIterable;
import xapi.fu.X_Fu;

import java.util.Arrays;

/**
 * @author Julio Vilmar Gesser
 */
public abstract class AnnotationExpr extends Expression {

    public static final AnnotationExpr NULLABLE = newMarkerAnnotation("Nullable");
    protected NameExpr name;

    public AnnotationExpr() {}

    public AnnotationExpr(
        int beginLine, int beginColumn, int endLine,
        int endColumn
    ) {
        super(beginLine, beginColumn, endLine, endColumn);
    }

    public NameExpr getName() {
        return name;
    }

    public String getNameString() {
        return name.getName();
    }

    public abstract MappedIterable<MemberValuePair> getMembers();

    public void setName(NameExpr name) {
        this.name = name;
        setAsParentNodeOf(name);
    }

    public static AnnotationExpr newMarkerAnnotation(String name) {
        final NameExpr nameExpr = NameExpr.of(name);
        return new MarkerAnnotationExpr(nameExpr);
    }

    public static AnnotationExpr newSingleMemberAnnotation(String name, Expression value) {
        final NameExpr nameExpr = NameExpr.of(name);
        return new SingleMemberAnnotationExpr(nameExpr, value);
    }

    public static AnnotationExpr newAnnotation(String name, MemberValuePair ... values) {
        if (X_Fu.isEmpty(values)) {
            return newMarkerAnnotation(name);
        }
        if (values.length == 1 && values[0].getName().equals("value")) {
            return newSingleMemberAnnotation(name, values[0].getValue());
        }
        return new NormalAnnotationExpr(NameExpr.of(name), Arrays.asList(values));
    }

}
