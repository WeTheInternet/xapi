
/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/22/16.
 */
/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 * Copyright (c) 2016-... WeTheInter.net on 10/22/16.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/22/16.
 *
 * Inside a UiExpr, the use of public private or protected
 * should allow one to insert a "member" object of raw java;
 * specifically a {@link BodyDeclaration} instance,
 * set to class mode (if you want to export interfaces,
 * do be sure to prefer `public default` in your expression,
 * as that is the only valid combination for java interfaces.
 *
 * Most generators would listen to the type information,
 * then generate what you expect, so a private void methodName(...)
 * declaration would be used to generate the correct access level for output code.
 * It is only pass-through java templating that you will want sensible defaults as best practices.
 *
 *
 * Original license:
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

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

public final class DynamicDeclarationExpr extends Expression {

    private Expression scope;

    private BodyDeclaration body;

    public DynamicDeclarationExpr() {

    }

    public DynamicDeclarationExpr(BodyDeclaration body) {
        this.body = body;
        if (body != null) {
            setBeginLine(body.getBeginLine());
            setBeginColumn(body.getBeginColumn());
            setEndLine(body.getEndLine());
            setEndColumn(body.getEndColumn());
        }
    }

    public BodyDeclaration getBody() {
        return body;
    }

    public void setBody(BodyDeclaration body) {
        this.body = body;
        setAsParentNodeOf(body);
    }

    public DynamicDeclarationExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                                  final BodyDeclaration body) {
        super(beginLine, beginColumn, endLine, endColumn);
        setBody(body);
    }

    @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    public Expression getScope() {
        return scope;
    }
}

