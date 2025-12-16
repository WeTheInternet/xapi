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
 * A visitor that has a return value.
 *
 * @author Julio Vilmar Gesser
 */
public interface GenericVisitor<R, A> {

	//- Compilation Unit ----------------------------------

	public R visit(CompilationUnit n, A arg);

	public R visit(PackageDeclaration n, A arg);

	public R visit(ImportDeclaration n, A arg);

	public R visit(TypeParameter n, A arg);

	public R visit(LineComment n, A arg);

	public R visit(BlockComment n, A arg);

	//- Body ----------------------------------------------

	public R visit(ClassOrInterfaceDeclaration n, A arg);

	public R visit(EnumDeclaration n, A arg);

	public R visit(EmptyTypeDeclaration n, A arg);

	public R visit(EnumConstantDeclaration n, A arg);

	public R visit(AnnotationDeclaration n, A arg);

	public R visit(AnnotationMemberDeclaration n, A arg);

	public R visit(FieldDeclaration n, A arg);

	public R visit(VariableDeclarator n, A arg);

	public R visit(VariableDeclaratorId n, A arg);

	public R visit(ConstructorDeclaration n, A arg);

	public R visit(MethodDeclaration n, A arg);

	public R visit(Parameter n, A arg);

	public R visit(MultiTypeParameter n, A arg);

	public R visit(EmptyMemberDeclaration n, A arg);

	public R visit(InitializerDeclaration n, A arg);

	public R visit(JavadocComment n, A arg);

	//- Type ----------------------------------------------

	public R visit(ClassOrInterfaceType n, A arg);

	public R visit(PrimitiveType n, A arg);

	public R visit(ReferenceType n, A arg);

    public R visit(IntersectionType n, A arg);

    public R visit(UnionType n, A arg);

	public R visit(VoidType n, A arg);

	public R visit(WildcardType n, A arg);

	public R visit(UnknownType n, A arg);

	//- Expression ----------------------------------------

	public R visit(ArrayAccessExpr n, A arg);

	public R visit(ArrayCreationExpr n, A arg);

	public R visit(ArrayInitializerExpr n, A arg);

	public R visit(AssignExpr n, A arg);

	public R visit(BinaryExpr n, A arg);

	public R visit(CastExpr n, A arg);

	public R visit(ClassExpr n, A arg);

	public R visit(ConditionalExpr n, A arg);

	public R visit(EnclosedExpr n, A arg);

	public R visit(FieldAccessExpr n, A arg);

	public R visit(InstanceOfExpr n, A arg);

	public R visit(StringLiteralExpr n, A arg);

	public R visit(TemplateLiteralExpr n, A arg);

	public R visit(UiBodyExpr n, A arg);

	public R visit(DynamicDeclarationExpr n, A arg);

	public R visit(IntegerLiteralExpr n, A arg);

	public R visit(LongLiteralExpr n, A arg);

	public R visit(IntegerLiteralMinValueExpr n, A arg);

	public R visit(LongLiteralMinValueExpr n, A arg);

	public R visit(CharLiteralExpr n, A arg);

	public R visit(DoubleLiteralExpr n, A arg);

	public R visit(BooleanLiteralExpr n, A arg);

	public R visit(NullLiteralExpr n, A arg);

	public R visit(MethodCallExpr n, A arg);

	public R visit(NameExpr n, A arg);

	public R visit(ObjectCreationExpr n, A arg);

	public R visit(QualifiedNameExpr n, A arg);

	public R visit(ThisExpr n, A arg);

	public R visit(SuperExpr n, A arg);

	public R visit(UnaryExpr n, A arg);

	public R visit(VariableDeclarationExpr n, A arg);

	public R visit(MarkerAnnotationExpr n, A arg);

	public R visit(SingleMemberAnnotationExpr n, A arg);

	public R visit(NormalAnnotationExpr n, A arg);

	public R visit(MemberValuePair n, A arg);

	//- Statements ----------------------------------------

	public R visit(ExplicitConstructorInvocationStmt n, A arg);

	public R visit(TypeDeclarationStmt n, A arg);

	public R visit(AssertStmt n, A arg);

	public R visit(BlockStmt n, A arg);

	public R visit(LabeledStmt n, A arg);

	public R visit(EmptyStmt n, A arg);

	public R visit(ExpressionStmt n, A arg);

	public R visit(SwitchStmt n, A arg);

	public R visit(SwitchEntryStmt n, A arg);

	public R visit(BreakStmt n, A arg);

	public R visit(ReturnStmt n, A arg);

	public R visit(IfStmt n, A arg);

	public R visit(WhileStmt n, A arg);

	public R visit(ContinueStmt n, A arg);

	public R visit(DoStmt n, A arg);

	public R visit(ForeachStmt n, A arg);

	public R visit(ForStmt n, A arg);

	public R visit(ThrowStmt n, A arg);

	public R visit(SynchronizedStmt n, A arg);

	public R visit(TryStmt n, A arg);

	public R visit(CatchClause n, A arg);

    public R visit(LambdaExpr n, A arg);

    public R visit(MethodReferenceExpr n, A arg);

    public R visit(TypeExpr n, A arg);

    R visit(UiAttrExpr n, A arg);

    R visit(UiContainerExpr n, A arg);

	R visit(JsonContainerExpr n, A arg);

	R visit(JsonPairExpr n, A arg);

	R visit(CssBlockExpr n, A arg);

	R visit(CssContainerExpr n, A arg);

	R visit(CssRuleExpr n, A arg);

	R visit(CssSelectorExpr n, A arg);

	R visit(CssValueExpr n, A arg);

    	R visit(SysExpr sysExpr, A arg);
}
