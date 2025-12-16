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

import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public final class QualifiedNameExpr extends NameExpr implements ScopedExpression {

	private NameExpr qualifier;

	public QualifiedNameExpr() {
	}

	public QualifiedNameExpr(final NameExpr scope, final String name) {
		super(name);
		setQualifier(scope);
	}

	public QualifiedNameExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
			final NameExpr scope, final String name) {
		super(beginLine, beginColumn, endLine, endColumn, name);
		setQualifier(scope);
	}

	public String getQualifiedName() {
		return getQualifier() == null ? getName() : getQualifier().getQualifiedName() + "." + getName();
	}

	@Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
		return v.visit(this, arg);
	}

	@Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
		v.visit(this, arg);
	}

	public NameExpr getQualifier() {
		return qualifier;
	}

	public void setQualifier(final NameExpr qualifier) {
		this.qualifier = qualifier;
		setAsParentNodeOf(this.qualifier);
	}

	@Override
	public Expression getScope() {
		return getQualifier();
	}

	@Override
	public void setScope(Expression scope) {
		assert scope instanceof NameExpr : "Qualified name expression can only use other NameExpr as scopes.  You sent " + scope;
		setQualifier((NameExpr) scope);
	}
}
