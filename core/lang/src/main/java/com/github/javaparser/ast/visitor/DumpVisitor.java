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

package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import xapi.fu.Printable;

import static com.github.javaparser.PositionUtils.sortByBeginPosition;
import static com.github.javaparser.ast.internal.Utils.isNullOrEmpty;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Dumps the AST to formatted Java source code.
 *
 * @author Julio Vilmar Gesser
 */
public class DumpVisitor implements VoidVisitor<Object> {

    private boolean printComments;

    public DumpVisitor() {
        this(true);
    }

    public DumpVisitor(boolean printComments) {
        this.printComments = printComments;
    }

	protected final Printable printer = createSourcePrinter();

	protected Printable createSourcePrinter() {
		return Printable.toPrinter(new StringBuilder());
	}

	public String getSource() {
		return printer.toSource();
	}

	private void printModifiers(final int modifiers) {
		if (ModifierSet.isPrivate(modifiers)) {
			printer.print("private ");
		}
		if (ModifierSet.isProtected(modifiers)) {
			printer.print("protected ");
		}
		if (ModifierSet.isPublic(modifiers)) {
			printer.print("public ");
		}
		if (ModifierSet.isAbstract(modifiers)) {
			printer.print("abstract ");
		}
		if (ModifierSet.isStatic(modifiers)) {
			printer.print("static ");
		}
		if (ModifierSet.isFinal(modifiers)) {
			printer.print("final ");
		}
		if (ModifierSet.isNative(modifiers)) {
			printer.print("native ");
		}
		if (ModifierSet.isStrictfp(modifiers)) {
			printer.print("strictfp ");
		}
		if (ModifierSet.isSynchronized(modifiers)) {
			printer.print("synchronized ");
		}
		if (ModifierSet.isTransient(modifiers)) {
			printer.print("transient ");
		}
		if (ModifierSet.isVolatile(modifiers)) {
			printer.print("volatile ");
		}
	}

	private void printMembers(final List<BodyDeclaration> members, final Object arg) {
		for (final BodyDeclaration member : members) {
			printer.println();
			member.accept(this, arg);
			printer.println();
		}
	}

	private void printMemberAnnotations(final List<AnnotationExpr> annotations, final Object arg) {
		if (!isNullOrEmpty(annotations)) {
			for (final AnnotationExpr a : annotations) {
				a.accept(this, arg);
				printer.println();
			}
		}
	}

	private void printAnnotations(final List<AnnotationExpr> annotations, final Object arg) {
		if (!isNullOrEmpty(annotations)) {
			for (final AnnotationExpr a : annotations) {
				a.accept(this, arg);
				printer.print(" ");
			}
		}
	}

	private void printTypeArgs(final List<Type> args, final Object arg) {
        if (!isNullOrEmpty(args)) {
			printer.print("<");
			for (final Iterator<Type> i = args.iterator(); i.hasNext();) {
				final Type t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
			printer.print(">");
		}
	}

	private void printTypeParameters(final List<TypeParameter> args, final Object arg) {
        if (!isNullOrEmpty(args)) {
			printer.print("<");
			for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
				final TypeParameter t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
			printer.print(">");
		}
	}

	private void printArguments(final List<Expression> args, final Object arg) {
		printer.print("(");
        if (!isNullOrEmpty(args)) {
			for (final Iterator<Expression> i = args.iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(")");
	}

	private void printJavadoc(final JavadocComment javadoc, final Object arg) {
		if (javadoc != null) {
			javadoc.accept(this, arg);
		}
	}

	protected void printJavaComment(final Comment javacomment, final Object arg) {
		if (javacomment != null) {
			javacomment.accept(this, arg);
		}
	}

	@Override public void visit(final CompilationUnit n, final Object arg) {
		printJavaComment(n.getComment(), arg);

		if (n.getPackage() != null) {
			n.getPackage().accept(this, arg);
		}

		if (!isNullOrEmpty(n.getImports())) {
			for (final ImportDeclaration i : n.getImports()) {
				i.accept(this, arg);
			}
			printer.println();
		}

		if (!isNullOrEmpty(n.getTypes())) {
			for (final Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
				i.next().accept(this, arg);
				printer.println();
				if (i.hasNext()) {
					printer.println();
				}
			}
		}

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final PackageDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printer.print("package ");
		n.getName().accept(this, arg);
		printer.println(";");
		printer.println();

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final NameExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final QualifiedNameExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getQualifier().accept(this, arg);
		printer.print(".");
		printer.print(n.getName());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final ImportDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("import ");
		if (n.isStatic()) {
			printer.print("static ");
		}
		n.getName().accept(this, arg);
		if (n.isAsterisk()) {
			printer.print(".*");
		}
		printer.println(";");

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final ClassOrInterfaceDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		if (n.isInterface()) {
			printer.print("interface ");
		} else {
			printer.print("class ");
		}

		printer.print(n.getName());

		printTypeParameters(n.getTypeParameters(), arg);

		if (!isNullOrEmpty(n.getExtends())) {
			printer.print(" extends ");
			for (final Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}

		if (!isNullOrEmpty(n.getImplements())) {
			printer.print(" implements ");
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}

		printer.println(" {");
		printer.indent();
		if (!isNullOrEmpty(n.getMembers())) {
			printMembers(n.getMembers(), arg);
		}

        printOrphanCommentsEnding(n);

		printer.outdent();
		printer.print("}");
	}

	@Override public void visit(final EmptyTypeDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printer.print(";");

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final JavadocComment n, final Object arg) {
		printer.print("/**");
		printer.print(n.getContent());
		printer.println("*/");
	}

	@Override public void visit(final ClassOrInterfaceType n, final Object arg) {
		printJavaComment(n.getComment(), arg);

		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ");
			}
		}

		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".");
		}
		printer.print(n.getName());

		if (n.isUsingDiamondOperator()) {
			printer.print("<>");
		} else {
			printTypeArgs(n.getTypeArgs(), arg);
		}
	}

	@Override public void visit(final TypeParameter n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ann : n.getAnnotations()) {
				ann.accept(this, arg);
				printer.print(" ");
			}
		}
		printer.print(n.getName());
		if (!isNullOrEmpty(n.getTypeBound())) {
			printer.print(" extends ");
			for (final Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
				}
			}
		}
	}

	@Override public void visit(final PrimitiveType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (!isNullOrEmpty(n.getAnnotations())) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ");
			}
		}
		switch (n.getType()) {
		case Boolean:
			printer.print("boolean");
			break;
		case Byte:
			printer.print("byte");
			break;
		case Char:
			printer.print("char");
			break;
		case Double:
			printer.print("double");
			break;
		case Float:
			printer.print("float");
			break;
		case Int:
			printer.print("int");
			break;
		case Long:
			printer.print("long");
			break;
		case Short:
			printer.print("short");
			break;
		}
	}

	@Override public void visit(final ReferenceType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (!isNullOrEmpty(n.getAnnotations())) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ");
			}
		}
		n.getType().accept(this, arg);
		List<List<AnnotationExpr>> arraysAnnotations = n.getArraysAnnotations();
		for (int i = 0; i < n.getArrayCount(); i++) {
			if (arraysAnnotations != null && i < arraysAnnotations.size()) {
				List<AnnotationExpr> annotations = arraysAnnotations.get(i);
				if (!isNullOrEmpty(annotations)) {
					for (AnnotationExpr ae : annotations) {
						printer.print(" ");
						ae.accept(this, arg);

					}
				}
			}
			printer.print("[]");
		}
	}

    @Override public void visit(final IntersectionType n, final Object arg) {
        printJavaComment(n.getComment(), arg);
        boolean isFirst = true;
        for (ReferenceType element : n.getElements()) {
            element.accept(this, arg);
            if (isFirst) {
                isFirst = false;
            } else {
                printer.print(" & ");
            }
        }
    }

    @Override public void visit(final UnionType n, final Object arg) {
        printJavaComment(n.getComment(), arg);
        boolean isFirst = true;
        for (ReferenceType element : n.getElements()) {
            element.accept(this, arg);
            if (isFirst) {
                isFirst = false;
            } else {
                printer.print(" | ");
            }
        }
    }


	@Override public void visit(final WildcardType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				printer.print(" ");
				ae.accept(this, arg);
			}
		}
		printer.print("?");
		if (n.getExtends() != null) {
			printer.print(" extends ");
			n.getExtends().accept(this, arg);
		}
		if (n.getSuper() != null) {
			printer.print(" super ");
			n.getSuper().accept(this, arg);
		}
	}

	@Override public void visit(final UnknownType n, final Object arg) {
		// Nothing to dump
	}

	@Override public void visit(final FieldDeclaration n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);

		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());
		n.getType().accept(this, arg);

		printer.print(" ");
		for (final Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
			final VariableDeclarator var = i.next();
			var.accept(this, arg);
			if (i.hasNext()) {
				printer.print(", ");
			}
		}

		printer.print(";");
	}

	@Override public void visit(final VariableDeclarator n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getId().accept(this, arg);
		if (n.getInit() != null) {
			printer.print(" = ");
			n.getInit().accept(this, arg);
		}
	}

	@Override public void visit(final VariableDeclaratorId n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName());
		for (int i = 0; i < n.getArrayCount(); i++) {
			printer.print("[]");
		}
	}

	@Override public void visit(final ArrayInitializerExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("{");
		if (!isNullOrEmpty(n.getValues())) {
			printer.print(" ");
			for (final Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
				final Expression expr = i.next();
				expr.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
			printer.print(" ");
		}
		printer.print("}");
	}

	@Override public void visit(final VoidType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("void");
	}

	@Override public void visit(final ArrayAccessExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getName().accept(this, arg);
		printer.print("[");
		n.getIndex().accept(this, arg);
		printer.print("]");
	}

	@Override public void visit(final ArrayCreationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("new ");
		n.getType().accept(this, arg);
		List<List<AnnotationExpr>> arraysAnnotations = n.getArraysAnnotations();
		if (!isNullOrEmpty(n.getDimensions())) {
			int j = 0;
			for (final Expression dim : n.getDimensions()) {

				if (arraysAnnotations != null && j < arraysAnnotations.size()) {
					List<AnnotationExpr> annotations = arraysAnnotations.get(j);
					if (!isNullOrEmpty(annotations)) {
						for (AnnotationExpr ae : annotations) {
							printer.print(" ");
							ae.accept(this, arg);
						}
					}
				}
				printer.print("[");
				dim.accept(this, arg);
				printer.print("]");
				j++;
			}
			for (int i = 0; i < n.getArrayCount(); i++) {
				if (arraysAnnotations != null && i < arraysAnnotations.size()) {

					List<AnnotationExpr> annotations = arraysAnnotations.get(i);
					if (!isNullOrEmpty(annotations)) {
						for (AnnotationExpr ae : annotations) {
							printer.print(" ");
							ae.accept(this, arg);

						}
					}
				}
				printer.print("[]");
			}

		} else {
			for (int i = 0; i < n.getArrayCount(); i++) {
				if (arraysAnnotations != null && i < arraysAnnotations.size()) {
					List<AnnotationExpr> annotations = arraysAnnotations.get(i);
					if (!isNullOrEmpty(annotations)) {
						for (AnnotationExpr ae : annotations) {
							ae.accept(this, arg);
							printer.print(" ");
						}
					}
				}
				printer.print("[]");
			}
			printer.print(" ");
			n.getInitializer().accept(this, arg);
		}
	}

	@Override public void visit(final AssignExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getTarget().accept(this, arg);
		printer.print(" ");
		switch (n.getOperator()) {
		case assign:
			printer.print("=");
			break;
		case and:
			printer.print("&=");
			break;
		case or:
			printer.print("|=");
			break;
		case xor:
			printer.print("^=");
			break;
		case plus:
			printer.print("+=");
			break;
		case minus:
			printer.print("-=");
			break;
		case rem:
			printer.print("%=");
			break;
		case slash:
			printer.print("/=");
			break;
		case star:
			printer.print("*=");
			break;
		case lShift:
			printer.print("<<=");
			break;
		case rSignedShift:
			printer.print(">>=");
			break;
		case rUnsignedShift:
			printer.print(">>>=");
			break;
		}
		printer.print(" ");
		n.getValue().accept(this, arg);
	}

	@Override public void visit(final BinaryExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getLeft().accept(this, arg);
		printer.print(" ");
		switch (n.getOperator()) {
		case or:
			printer.print("||");
			break;
		case and:
			printer.print("&&");
			break;
		case binOr:
			printer.print("|");
			break;
		case binAnd:
			printer.print("&");
			break;
		case xor:
			printer.print("^");
			break;
		case equals:
			printer.print("==");
			break;
		case notEquals:
			printer.print("!=");
			break;
		case less:
			printer.print("<");
			break;
		case greater:
			printer.print(">");
			break;
		case lessEquals:
			printer.print("<=");
			break;
		case greaterEquals:
			printer.print(">=");
			break;
		case lShift:
			printer.print("<<");
			break;
		case rSignedShift:
			printer.print(">>");
			break;
		case rUnsignedShift:
			printer.print(">>>");
			break;
		case plus:
			printer.print("+");
			break;
		case minus:
			printer.print("-");
			break;
		case times:
			printer.print("*");
			break;
		case divide:
			printer.print("/");
			break;
		case remainder:
			printer.print("%");
			break;
		}
		printer.print(" ");
		n.getRight().accept(this, arg);
	}

	@Override public void visit(final CastExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("(");
		n.getType().accept(this, arg);
		printer.print(") ");
		n.getExpr().accept(this, arg);
	}

	@Override public void visit(final ClassExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getType().accept(this, arg);
		printer.print(".class");
	}

	@Override public void visit(final ConditionalExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getCondition().accept(this, arg);
		printer.print(" ? ");
		n.getThenExpr().accept(this, arg);
		printer.print(" : ");
		n.getElseExpr().accept(this, arg);
	}

	@Override public void visit(final EnclosedExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("(");
		if (n.getInner() != null) {
		n.getInner().accept(this, arg);
		}
		printer.print(")");
	}

	@Override public void visit(final FieldAccessExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getScope().accept(this, arg);
		printer.print(".");
		printer.print(n.getField());
	}

	@Override public void visit(final InstanceOfExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getExpr().accept(this, arg);
		printer.print(" instanceof ");
		n.getType().accept(this, arg);
	}

	@Override public void visit(final CharLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("'");
		printer.print(n.getValue());
		printer.print("'");
	}

	@Override public void visit(final DoubleLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue());
	}

	@Override public void visit(final IntegerLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue());
	}

	@Override public void visit(final LongLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue());
	}

	@Override public void visit(final IntegerLiteralMinValueExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue());
	}

	@Override public void visit(final LongLiteralMinValueExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue());
	}

	@Override public void visit(final StringLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("\"");
		printer.print(n.getValue());
		printer.print("\"");
	}

	@Override public void visit(final TemplateLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
        // TODO: resolve tags to print valid, escaped java code.
		printer.print("`");
		printer.print(n.getValue());
		printer.print("`");
	}

    @Override
    public void visit(UiAttrExpr n, Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.indent();
		n.getName().accept(this, arg);
		printer.print(" = ");
        if (n.getAnnotations() != null) {
            printer.println();
          for (AnnotationExpr annotationExpr : n.getAnnotations()) {
            annotationExpr.accept(this, arg);
            printer.println();
          }
          printer.println();
        }
		n.getExpression().accept(this, arg);
		printer.outdent();
    }

    @Override
    public void visit(UiContainerExpr n, Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.isInTemplate()) {
			printer.print("`");
		}
		printer.print("<");
		n.getNameExpr().accept(this, arg);
		printer.indent();
		for (UiAttrExpr attr : n.getAttributes()) {
			printer.println();
			attr.accept(this, arg);
		}
		printer.outdent();
		if (n.getBody() == null) {
			printer.print("/>");
		} else {
			printer.print(">");
			n.getBody().accept(this, arg);
			printer.print("</");
			n.getNameExpr().accept(this, arg);
			printer.print(">");
		}
		if (n.isInTemplate()) {
			printer.print("`");
		}
    }

    @Override
    public void visit(UiBodyExpr n, Object arg) {
		printJavaComment(n.getComment(), arg);
		for (UiExpr child : n.getChildren()) {
			if (child instanceof TemplateLiteralExpr) {
				printer.print(((TemplateLiteralExpr)child).getValue());
			} else {
				child.accept(this, arg);
			}
		}

	}

  @Override public void visit(final BooleanLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(String.valueOf(n.getValue()));
	}

	@Override public void visit(final NullLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("null");
	}

	@Override public void visit(final ThisExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			printer.print(".");
		}
		printer.print("this");
	}

	@Override public void visit(final SuperExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			printer.print(".");
		}
		printer.print("super");
	}

	@Override public void visit(final MethodCallExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".");
		}
		printTypeArgs(n.getTypeArgs(), arg);
		printer.print(n.getName());
		printArguments(n.getArgs(), arg);
	}

	@Override public void visit(final ObjectCreationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".");
		}

		printer.print("new ");

		printTypeArgs(n.getTypeArgs(), arg);
		if (!isNullOrEmpty(n.getTypeArgs())) {
			printer.print(" ");
		}

		n.getType().accept(this, arg);

		printArguments(n.getArgs(), arg);

		if (n.getAnonymousClassBody() != null) {
			printer.println(" {");
			printer.indent();
			printMembers(n.getAnonymousClassBody(), arg);
			printer.outdent();
			printer.print("}");
		}
	}

	@Override public void visit(final UnaryExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		switch (n.getOperator()) {
		case positive:
			printer.print("+");
			break;
		case negative:
			printer.print("-");
			break;
		case inverse:
			printer.print("~");
			break;
		case not:
			printer.print("!");
			break;
		case preIncrement:
			printer.print("++");
			break;
		case preDecrement:
			printer.print("--");
			break;
		default:
		}

		n.getExpr().accept(this, arg);

		switch (n.getOperator()) {
		case posIncrement:
			printer.print("++");
			break;
		case posDecrement:
			printer.print("--");
			break;
		default:
		}
	}

	@Override public void visit(final ConstructorDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		printTypeParameters(n.getTypeParameters(), arg);
		if (!n.getTypeParameters().isEmpty()) {
			printer.print(" ");
		}
		printer.print(n.getName());

		printer.print("(");
		if (!n.getParameters().isEmpty()) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(")");

		if (!isNullOrEmpty(n.getThrows())) {
			printer.print(" throws ");
			for (final Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
				final NameExpr name = i.next();
				name.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(" ");
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final MethodDeclaration n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);

		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());
		if (n.isDefault()) {
			printer.print("default ");
		}
		printTypeParameters(n.getTypeParameters(), arg);
		if (!isNullOrEmpty(n.getTypeParameters())) {
			printer.print(" ");
		}

		n.getType().accept(this, arg);
		printer.print(" ");
		printer.print(n.getName());

		printer.print("(");
		if (!isNullOrEmpty(n.getParameters())) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(")");

		for (int i = 0; i < n.getArrayCount(); i++) {
			printer.print("[]");
		}

		if (!isNullOrEmpty(n.getThrows())) {
			printer.print(" throws ");
			for (final Iterator<ReferenceType> i = n.getThrows().iterator(); i.hasNext();) {
				final ReferenceType name = i.next();
				name.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		if (n.getBody() == null) {
			printer.print(";");
		} else {
			printer.print(" ");
			n.getBody().accept(this, arg);
		}
	}

	@Override public void visit(final Parameter n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());
		if (n.getType() != null) {
			n.getType().accept(this, arg);
		}
		if (n.isVarArgs()) {
			printer.print("...");
		}
		printer.print(" ");
		n.getId().accept(this, arg);
	}

    @Override public void visit(MultiTypeParameter n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        Type type = n.getType();
        if (type != null) {
        	type.accept(this, arg);
        }

        printer.print(" ");
        n.getId().accept(this, arg);
    }

	@Override public void visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.isThis()) {
			printTypeArgs(n.getTypeArgs(), arg);
			printer.print("this");
		} else {
			if (n.getExpr() != null) {
				n.getExpr().accept(this, arg);
				printer.print(".");
			}
			printTypeArgs(n.getTypeArgs(), arg);
			printer.print("super");
		}
		printArguments(n.getArgs(), arg);
		printer.print(";");
	}

	@Override public void visit(final VariableDeclarationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		n.getType().accept(this, arg);
		printer.print(" ");

		for (final Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
			final VariableDeclarator v = i.next();
			v.accept(this, arg);
			if (i.hasNext()) {
				printer.print(", ");
			}
		}
	}

	@Override public void visit(final TypeDeclarationStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getTypeDeclaration().accept(this, arg);
	}

	@Override public void visit(final AssertStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("assert ");
		n.getCheck().accept(this, arg);
		if (n.getMessage() != null) {
			printer.print(" : ");
			n.getMessage().accept(this, arg);
		}
		printer.print(";");
	}

	@Override public void visit(final BlockStmt n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);
		printJavaComment(n.getComment(), arg);
		printer.println("{");
		if (n.getStmts() != null) {
			printer.indent();
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
				printer.println();
			}
			printer.outdent();
		}
		printOrphanCommentsEnding(n);
		printer.print("}");

	}

	@Override public void visit(final LabeledStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getLabel());
		printer.print(": ");
		n.getStmt().accept(this, arg);
	}

	@Override public void visit(final EmptyStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(";");
	}

	@Override public void visit(final ExpressionStmt n, final Object arg) {
		printOrphanCommentsBeforeThisChildNode(n);
		printJavaComment(n.getComment(), arg);
		n.getExpression().accept(this, arg);
		printer.print(";");
	}

	@Override public void visit(final SwitchStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("switch(");
		n.getSelector().accept(this, arg);
		printer.println(") {");
		if (n.getEntries() != null) {
			printer.indent();
			for (final SwitchEntryStmt e : n.getEntries()) {
				e.accept(this, arg);
			}
			printer.outdent();
		}
		printer.print("}");

	}

	@Override public void visit(final SwitchEntryStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getLabel() != null) {
			printer.print("case ");
			n.getLabel().accept(this, arg);
			printer.print(":");
		} else {
			printer.print("default:");
		}
		printer.println();
		printer.indent();
		if (n.getStmts() != null) {
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
				printer.println();
			}
		}
		printer.outdent();
	}

	@Override public void visit(final BreakStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("break");
		if (n.getId() != null) {
			printer.print(" ");
			printer.print(n.getId());
		}
		printer.print(";");
	}

	@Override public void visit(final ReturnStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("return");
		if (n.getExpr() != null) {
			printer.print(" ");
			n.getExpr().accept(this, arg);
		}
		printer.print(";");
	}

	@Override public void visit(final EnumDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		printer.print("enum ");
		printer.print(n.getName());

		if (!n.getImplements().isEmpty()) {
			printer.print(" implements ");
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}

		printer.println(" {");
		printer.indent();
		if (n.getEntries() != null) {
			printer.println();
			for (final Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
				final EnumConstantDeclaration e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		if (!n.getMembers().isEmpty()) {
			printer.println(";");
			printMembers(n.getMembers(), arg);
		} else {
			if (!n.getEntries().isEmpty()) {
				printer.println();
			}
		}
		printer.outdent();
		printer.print("}");
	}

	@Override public void visit(final EnumConstantDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printer.print(n.getName());

		if (n.getArgs() != null) {
			printArguments(n.getArgs(), arg);
		}

		if (!n.getClassBody().isEmpty()) {
			printer.println(" {");
			printer.indent();
			printMembers(n.getClassBody(), arg);
			printer.outdent();
			printer.println("}");
		}
	}

	@Override public void visit(final EmptyMemberDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printer.print(";");
	}

	@Override public void visit(final InitializerDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		if (n.isStatic()) {
			printer.print("static ");
		}
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final IfStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("if (");
		n.getCondition().accept(this, arg);
		final boolean thenBlock = n.getThenStmt() instanceof BlockStmt;
		if (thenBlock) // block statement should start on the same line
			printer.print(") ");
		else {
			printer.println(")");
			printer.indent();
		}
		n.getThenStmt().accept(this, arg);
		if (!thenBlock)
			printer.outdent();
		if (n.getElseStmt() != null) {
			if (thenBlock)
				printer.print(" ");
			else
				printer.println();
			final boolean elseIf = n.getElseStmt() instanceof IfStmt;
			final boolean elseBlock = n.getElseStmt() instanceof BlockStmt;
			if (elseIf || elseBlock) // put chained if and start of block statement on a same level
				printer.print("else ");
			else {
				printer.println("else");
				printer.indent();
			}
			n.getElseStmt().accept(this, arg);
			if (!(elseIf || elseBlock))
				printer.outdent();
		}
	}

	@Override public void visit(final WhileStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("while (");
		n.getCondition().accept(this, arg);
		printer.print(") ");
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ContinueStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("continue");
		if (n.getId() != null) {
			printer.print(" ");
			printer.print(n.getId());
		}
		printer.print(";");
	}

	@Override public void visit(final DoStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("do ");
		n.getBody().accept(this, arg);
		printer.print(" while (");
		n.getCondition().accept(this, arg);
		printer.print(");");
	}

	@Override public void visit(final ForeachStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("for (");
		n.getVariable().accept(this, arg);
		printer.print(" : ");
		n.getIterable().accept(this, arg);
		printer.print(") ");
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ForStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("for (");
		if (n.getInit() != null) {
			for (final Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print("; ");
		if (n.getCompare() != null) {
			n.getCompare().accept(this, arg);
		}
		printer.print("; ");
		if (n.getUpdate() != null) {
			for (final Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(") ");
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ThrowStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("throw ");
		n.getExpr().accept(this, arg);
		printer.print(";");
	}

	@Override public void visit(final SynchronizedStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("synchronized (");
		n.getExpr().accept(this, arg);
		printer.print(") ");
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final TryStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("try ");
		if (!n.getResources().isEmpty()) {
			printer.print("(");
			Iterator<VariableDeclarationExpr> resources = n.getResources().iterator();
			boolean first = true;
			while (resources.hasNext()) {
				visit(resources.next(), arg);
				if (resources.hasNext()) {
					printer.print(";");
					printer.println();
					if (first) {
						printer.indent();
					}
				}
				first = false;
			}
			if (n.getResources().size() > 1) {
				printer.outdent();
			}
			printer.print(") ");
		}
		n.getTryBlock().accept(this, arg);
		if (n.getCatchs() != null) {
			for (final CatchClause c : n.getCatchs()) {
				c.accept(this, arg);
			}
		}
		if (n.getFinallyBlock() != null) {
			printer.print(" finally ");
			n.getFinallyBlock().accept(this, arg);
		}
	}

	@Override public void visit(final CatchClause n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(" catch (");
		n.getParam().accept(this, arg);
		printer.print(") ");
		n.getCatchBlock().accept(this, arg);

	}

	@Override public void visit(final AnnotationDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		printer.print("@interface ");
		printer.print(n.getName());
		printer.println(" {");
		printer.indent();
		if (n.getMembers() != null) {
			printMembers(n.getMembers(), arg);
		}
		printer.outdent();
		printer.print("}");
	}

	@Override public void visit(final AnnotationMemberDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers());

		n.getType().accept(this, arg);
		printer.print(" ");
		printer.print(n.getName());
		printer.print("()");
		if (n.getDefaultValue() != null) {
			printer.print(" default ");
			n.getDefaultValue().accept(this, arg);
		}
		printer.print(";");
	}

	@Override public void visit(final MarkerAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@");
		n.getName().accept(this, arg);
	}

	@Override public void visit(final SingleMemberAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@");
		n.getName().accept(this, arg);
		printer.print("(");
		n.getMemberValue().accept(this, arg);
		printer.print(")");
	}

	@Override public void visit(final NormalAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@");
		n.getName().accept(this, arg);
		printer.print("(");
		if (n.getPairs() != null) {
			for (final Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext();) {
				final MemberValuePair m = i.next();
				m.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(")");
	}

	@Override public void visit(final MemberValuePair n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName());
		printer.print(" = ");
		n.getValue().accept(this, arg);
	}

	@Override public void visit(final LineComment n, final Object arg) {
		if (!this.printComments) {
            return;
        }
        printer.print("//");
		String tmp = n.getContent();
		tmp = tmp.replace('\r', ' ');
		tmp = tmp.replace('\n', ' ');
		printer.println(tmp);
	}

	@Override public void visit(final BlockComment n, final Object arg) {
        if (!this.printComments) {
            return;
        }
        printer.print("/*");
		printer.print(n.getContent());
		printer.println("*/");
	}

	@Override
	public void visit(LambdaExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);

        List<Parameter> parameters = n.getParameters();
		boolean printPar = n.isParametersEnclosed();

		if (printPar) {
			printer.print("(");
		}
		boolean hadParam = false;
		if (parameters != null) {
			for (Iterator<Parameter> i = parameters.iterator(); i.hasNext();) {
				hadParam = true;
				Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		if (printPar) {
			if (hadParam) {
				printer.print(" ");
			}
			printer.print(")");
		}

		printer.print(" -> ");
		Statement body = n.getBody();
		if (body instanceof ExpressionStmt) {
			// Print the expression directly
			((ExpressionStmt) body).getExpression().accept(this, arg);
		} else {
			body.accept(this, arg);
		}
	}


    @Override
    public void visit(MethodReferenceExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);
        Expression scope = n.getScope();
        String identifier = n.getIdentifier();
        if (scope != null) {
            n.getScope().accept(this, arg);
        }

        printer.print("::");
        if (!n.getTypeParameters().isEmpty()) {
            printer.print("<");
            for (Iterator<TypeParameter> i = n.getTypeParameters().iterator(); i
                    .hasNext();) {
                TypeParameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
        if (identifier != null) {
            printer.print(identifier);
        }

    }

    @Override
    public void visit(TypeExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getType() != null) {
            n.getType().accept(this, arg);
        }
    }

  private boolean inArray;
  @Override
  public void visit(JsonContainerExpr n, Object arg) {
    if (n.isArray()) {
      printer.print("[");
    } else {
      printer.print("{");
    }

    final boolean was = inArray;
    inArray = n.isArray();
    final List<JsonPairExpr> pairs = n.getPairs();
    for (int i = 0; i < pairs.size(); i++ ) {
      if (i > 0) {
        printer.print(", ");
      }
      pairs.get(i).accept(this, arg);
    }
    inArray = was;

    if (n.isArray()) {
      printer.print("]");
    } else {
      printer.print("}");
    }
  }

  @Override
  public void visit(JsonPairExpr n, Object arg) {
    if (inArray) {
      assert n.getKeyExpr() instanceof IntegerLiteralExpr;
      n.getValueExpr().accept(this, arg);
    } else {
      n.getKeyExpr().accept(this, arg);
      printer.print(" : ");
      n.getValueExpr().accept(this, arg);
    }
  }

  @Override
  public void visit(CssBlockExpr n, Object arg) {
    printer.println(".{");
    printer.indent();
    final List<CssContainerExpr> containers = n.getContainers();
    if (containers.size() == 1 && containers.get(0).getSelectors().isEmpty()) {
        // In the case of a single container with no rules, we do not want to
        // print a wrapper .{ } as there are no blocks inside; instead,
        // we just want to print .{ rules: inline }
        containers.get(0).getRules().forEach(rule -> visit(rule, arg));
    } else {
        containers.forEach(container->visit(container, arg));
    }
    printer.outdent();
    printer.print("}");
  }

  @Override
  public void visit(CssContainerExpr n, Object arg) {
    final List<CssSelectorExpr> selectors = n.getSelectors();
    for (int i = 0; i < selectors.size(); i++) {
      if (i > 0) {
        printer.println(", ");
      }
      selectors.get(i).accept(this, arg);
    }
    printer.println("{");
    printer.indent();
    n.getRules().forEach(rule->rule.accept(this, arg));
    printer.outdent();
    printer.println("}");

  }

  @Override
  public void visit(CssRuleExpr n, Object arg) {
    n.getKey().accept(this, arg);
    printer.print(" : ");
    n.getValue().accept(this, arg);
    printer.println(";");
  }

    @Override
    public void visit(CssValueExpr n, Object arg) {
        final Expression value = n.getValue();
        if (value != null) {
            value.accept(this, arg);
        }
        if (n.getUnit() != null) {
            printer.print(n.getUnit());
        }
        if (n.isImportant()) {
            printer.print(" !important");
        }
    }


	@Override
  public void visit(CssSelectorExpr n, Object arg) {
    n.getParts().forEach(part->printer.print(part+" "));
  }

  private void printOrphanCommentsBeforeThisChildNode(final Node node){
        if (node instanceof Comment) return;

        Node parent = node.getParentNode();
        if (parent==null) return;
        List<Node> everything = new LinkedList<>();
        everything.addAll(parent.getChildrenNodes());
        sortByBeginPosition(everything);
        int positionOfTheChild = -1;
        for (int i=0;i<everything.size();i++){
            if (everything.get(i)==node) positionOfTheChild=i;
        }
        if (positionOfTheChild==-1) throw new RuntimeException("My index not found!!! "+node);
        int positionOfPreviousChild = -1;
        for (int i=positionOfTheChild-1;i>=0 && positionOfPreviousChild==-1;i--){
            if (!(everything.get(i) instanceof Comment)) positionOfPreviousChild = i;
        }
        for (int i=positionOfPreviousChild+1;i<positionOfTheChild;i++){
            Node nodeToPrint = everything.get(i);
            if (!(nodeToPrint instanceof Comment)) throw new RuntimeException("Expected comment, instead "+nodeToPrint.getClass()+". Position of previous child: "+positionOfPreviousChild+", position of child "+positionOfTheChild);
            nodeToPrint.accept(this,null);
        }
    }


    private void printOrphanCommentsEnding(final Node node){
        List<Node> everything = new LinkedList<>();
        everything.addAll(node.getChildrenNodes());
        sortByBeginPosition(everything);
        if (everything.isEmpty()) {
            return;
        }

        int commentsAtEnd = 0;
        boolean findingComments = true;
        while (findingComments && commentsAtEnd<everything.size()){
            Node last = everything.get(everything.size() - 1 - commentsAtEnd);
            findingComments = (last instanceof Comment);
            if (findingComments) {
                commentsAtEnd++;
            }
        }
        for (int i=0; i<commentsAtEnd; i++){
            everything.get(everything.size()-commentsAtEnd+i).accept(this, null);
        }
    }

}
