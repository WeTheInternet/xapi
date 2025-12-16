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

package net.wti.lang.parser.ast.expr;

import net.wti.lang.parser.ast.NamedNode;
import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public class NameExpr extends Expression implements NamedNode {

    public static final String COMMENT_NAME = "--comment--";

    private String name;

    public NameExpr() {
    }

    public NameExpr(final String name) {
        this.name = name;
    }

    public NameExpr(
        final int beginLine, final int beginColumn, final int endLine, final int endColumn,
        final String name
    ) {
        super(beginLine, beginColumn, endLine, endColumn);
        this.name = name;
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Override
    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public String getQualifiedName() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NameExpr))
            return false;
        if (!super.equals(o))
            return false;

        final NameExpr nameExpr = (NameExpr) o;

        return name != null ? name.equals(nameExpr.name) : nameExpr.name == null;

    }

    public String getSimpleName() {
        String n = getName();
        if (n == null) {
            return null;
        }
        String[] parts = n.split("[.]");
        return parts[parts.length - 1];
    }

    public static NameExpr of(String name) {
        assert name != null : "Do not create NameExpr w/ a null name";
        int ind = name.indexOf('.');
        if (ind == -1) {
            return new NameExpr(name);
        }
        QualifiedNameExpr scope = null;
        int start = 0;
        while (ind != -1) {
            scope = new QualifiedNameExpr(scope, name.substring(start, ind));
            ind = name.indexOf('.', start = ind+1);
        }
        return new QualifiedNameExpr(scope, name.substring(start));
    }
}
