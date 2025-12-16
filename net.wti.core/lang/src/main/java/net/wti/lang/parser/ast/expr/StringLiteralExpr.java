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
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;

/**
 * @author Julio Vilmar Gesser
 */
public class StringLiteralExpr extends LiteralExpr {

	private Out1<String> value;

	public StringLiteralExpr() {
        this.value = Immutable.EMPTY_STRING;
	}

	public StringLiteralExpr(final String value) {
        if (value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException("Illegal literal expression: newlines (line feed or carriage return) have to be escaped");
        }
		setValue(value);
	}

	public StringLiteralExpr(final Out1<String> value) {
		setValueFactory(value);
	}

	public StringLiteralExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
			final String value) {
		super(beginLine, beginColumn, endLine, endColumn);
		setValue(value);
	}

	@Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
		return v.visit(this, arg);
	}

	@Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
		v.visit(this, arg);
	}

	public final String getValue() {
		return value.out1();
	}

	public final void setValue(final String value) {
		this.value = Immutable.immutable1(value);
	}

	public final void setValueFactory(final Out1<String> value) {
		this.value = Lazy.deferred1(value);
	}

	public static StringLiteralExpr stringLiteral(String name) {
		return new StringLiteralExpr(name);
	}

	public static StringLiteralExpr stringLiteral(Out1<String> name) {
		return new StringLiteralExpr(name);
	}
}
