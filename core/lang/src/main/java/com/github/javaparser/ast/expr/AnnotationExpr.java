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

/**
 * @author Julio Vilmar Gesser
 */
public abstract class AnnotationExpr extends Expression {

	protected NameExpr name;

	public AnnotationExpr() {}

	public AnnotationExpr(int beginLine, int beginColumn, int endLine,
			int endColumn) {
		super(beginLine, beginColumn, endLine, endColumn);
	}

	public NameExpr getName() {
		return name;
	}

    public abstract Iterable<MemberValuePair> getMembers();

	public void setName(NameExpr name) {
		this.name = name;
		setAsParentNodeOf(name);
	}
}
