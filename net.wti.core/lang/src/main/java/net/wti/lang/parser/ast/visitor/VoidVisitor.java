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

package net.wti.lang.parser.ast.visitor;

import net.wti.lang.parser.ast.comments.BlockComment;
import net.wti.lang.parser.ast.CompilationUnit;
import net.wti.lang.parser.ast.ImportDeclaration;
import net.wti.lang.parser.ast.comments.LineComment;
import net.wti.lang.parser.ast.PackageDeclaration;
import net.wti.lang.parser.ast.TypeParameter;
import net.wti.lang.parser.ast.body.AnnotationDeclaration;
import net.wti.lang.parser.ast.body.AnnotationMemberDeclaration;
import net.wti.lang.parser.ast.body.ClassOrInterfaceDeclaration;
import net.wti.lang.parser.ast.body.ConstructorDeclaration;
import net.wti.lang.parser.ast.body.EmptyMemberDeclaration;
import net.wti.lang.parser.ast.body.EmptyTypeDeclaration;
import net.wti.lang.parser.ast.body.EnumConstantDeclaration;
import net.wti.lang.parser.ast.body.EnumDeclaration;
import net.wti.lang.parser.ast.body.FieldDeclaration;
import net.wti.lang.parser.ast.body.InitializerDeclaration;
import net.wti.lang.parser.ast.comments.JavadocComment;
import net.wti.lang.parser.ast.body.MethodDeclaration;
import net.wti.lang.parser.ast.body.MultiTypeParameter;
import net.wti.lang.parser.ast.body.Parameter;
import net.wti.lang.parser.ast.body.VariableDeclarator;
import net.wti.lang.parser.ast.body.VariableDeclaratorId;
import net.wti.lang.parser.ast.stmt.AssertStmt;
import net.wti.lang.parser.ast.stmt.BlockStmt;
import net.wti.lang.parser.ast.stmt.BreakStmt;
import net.wti.lang.parser.ast.stmt.CatchClause;
import net.wti.lang.parser.ast.stmt.ContinueStmt;
import net.wti.lang.parser.ast.stmt.DoStmt;
import net.wti.lang.parser.ast.stmt.EmptyStmt;
import net.wti.lang.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import net.wti.lang.parser.ast.stmt.ExpressionStmt;
import net.wti.lang.parser.ast.stmt.ForStmt;
import net.wti.lang.parser.ast.stmt.ForeachStmt;
import net.wti.lang.parser.ast.stmt.IfStmt;
import net.wti.lang.parser.ast.stmt.LabeledStmt;
import net.wti.lang.parser.ast.stmt.ReturnStmt;
import net.wti.lang.parser.ast.stmt.SwitchEntryStmt;
import net.wti.lang.parser.ast.stmt.SwitchStmt;
import net.wti.lang.parser.ast.stmt.SynchronizedStmt;
import net.wti.lang.parser.ast.stmt.ThrowStmt;
import net.wti.lang.parser.ast.stmt.TryStmt;
import net.wti.lang.parser.ast.stmt.TypeDeclarationStmt;
import net.wti.lang.parser.ast.stmt.WhileStmt;
import net.wti.lang.parser.ast.expr.*;
import net.wti.lang.parser.ast.type.*;

/**
 * A visitor that does not return anything.
 *
 * @author Julio Vilmar Gesser
 */
public interface VoidVisitor<A> {

	//- Compilation Unit ----------------------------------

	void visit(CompilationUnit n, A arg);

	void visit(PackageDeclaration n, A arg);

	void visit(ImportDeclaration n, A arg);

	void visit(TypeParameter n, A arg);

	void visit(LineComment n, A arg);

	void visit(BlockComment n, A arg);

	//- Body ----------------------------------------------

	void visit(ClassOrInterfaceDeclaration n, A arg);

	void visit(EnumDeclaration n, A arg);

	void visit(EmptyTypeDeclaration n, A arg);

	void visit(EnumConstantDeclaration n, A arg);

	void visit(AnnotationDeclaration n, A arg);

	void visit(AnnotationMemberDeclaration n, A arg);

	void visit(FieldDeclaration n, A arg);

	void visit(VariableDeclarator n, A arg);

	void visit(VariableDeclaratorId n, A arg);

	void visit(ConstructorDeclaration n, A arg);

	void visit(MethodDeclaration n, A arg);

	void visit(Parameter n, A arg);

	void visit(MultiTypeParameter n, A arg);

	void visit(EmptyMemberDeclaration n, A arg);

	void visit(InitializerDeclaration n, A arg);

	void visit(JavadocComment n, A arg);

	//- Type ----------------------------------------------

	void visit(ClassOrInterfaceType n, A arg);

	void visit(PrimitiveType n, A arg);

	void visit(ReferenceType n, A arg);

    void visit(IntersectionType n, A arg);

    void visit(UnionType n, A arg);

	void visit(VoidType n, A arg);

	void visit(WildcardType n, A arg);

	void visit(UnknownType n, A arg);

	//- Expression ----------------------------------------

	void visit(ArrayAccessExpr n, A arg);

	void visit(ArrayCreationExpr n, A arg);

	void visit(ArrayInitializerExpr n, A arg);

	void visit(AssignExpr n, A arg);

	void visit(BinaryExpr n, A arg);

	void visit(CastExpr n, A arg);

	void visit(ClassExpr n, A arg);

	void visit(ConditionalExpr n, A arg);

	void visit(EnclosedExpr n, A arg);

	void visit(FieldAccessExpr n, A arg);

	void visit(InstanceOfExpr n, A arg);

	void visit(StringLiteralExpr n, A arg);

	void visit(TemplateLiteralExpr n, A arg);

	void visit(IntegerLiteralExpr n, A arg);

	void visit(LongLiteralExpr n, A arg);

	void visit(IntegerLiteralMinValueExpr n, A arg);

	void visit(LongLiteralMinValueExpr n, A arg);

	void visit(CharLiteralExpr n, A arg);

	void visit(DoubleLiteralExpr n, A arg);

	void visit(BooleanLiteralExpr n, A arg);

	void visit(NullLiteralExpr n, A arg);

	void visit(MethodCallExpr n, A arg);

	void visit(NameExpr n, A arg);

	void visit(ObjectCreationExpr n, A arg);

	void visit(QualifiedNameExpr n, A arg);

	void visit(ThisExpr n, A arg);

	void visit(SuperExpr n, A arg);

	void visit(UnaryExpr n, A arg);

	void visit(VariableDeclarationExpr n, A arg);

	void visit(MarkerAnnotationExpr n, A arg);

	void visit(SingleMemberAnnotationExpr n, A arg);

	void visit(NormalAnnotationExpr n, A arg);

	void visit(MemberValuePair n, A arg);

	//- Statements ----------------------------------------

	void visit(ExplicitConstructorInvocationStmt n, A arg);

	void visit(TypeDeclarationStmt n, A arg);

	void visit(AssertStmt n, A arg);

	void visit(BlockStmt n, A arg);

	void visit(LabeledStmt n, A arg);

	void visit(EmptyStmt n, A arg);

	void visit(ExpressionStmt n, A arg);

	void visit(SwitchStmt n, A arg);

	void visit(SwitchEntryStmt n, A arg);

	void visit(BreakStmt n, A arg);

	void visit(ReturnStmt n, A arg);

	void visit(IfStmt n, A arg);

	void visit(WhileStmt n, A arg);

	void visit(ContinueStmt n, A arg);

	void visit(DoStmt n, A arg);

	void visit(ForeachStmt n, A arg);

	void visit(ForStmt n, A arg);

	void visit(ThrowStmt n, A arg);

	void visit(SynchronizedStmt n, A arg);

	void visit(TryStmt n, A arg);

	void visit(CatchClause n, A arg);

    void visit(LambdaExpr n, A arg);

    void visit(MethodReferenceExpr n, A arg);

    void visit(TypeExpr n, A arg);

    void visit(DynamicDeclarationExpr n, A arg);

    void visit(UiAttrExpr n, A arg);

    void visit(UiContainerExpr n, A arg);

    void visit(UiBodyExpr n, A arg);

    void visit(JsonContainerExpr n, A arg);

    void visit(JsonPairExpr n, A arg);

    void visit(CssBlockExpr n, A arg);

    void visit(CssContainerExpr n, A arg);

    void visit(CssRuleExpr n, A arg);

    void visit(CssSelectorExpr n, A arg);

    void visit(CssValueExpr n, A arg);

    void visit(SysExpr sysExpr, A arg);
}
