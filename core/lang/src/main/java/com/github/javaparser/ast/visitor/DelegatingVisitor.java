package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import xapi.collect.api.ClassTo;
import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter1Unsafe;

import static xapi.collect.X_Collect.newClassMap;

import java.lang.reflect.Method;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/17/16.
 */
public class DelegatingVisitor <T, C extends DelegatingVisitor.Context<T>> extends GenericVisitorAdapter<Boolean, C> {

  protected Filter1<Node> filter;
  protected final ClassTo<Filter1Unsafe<Node>> nodeFilter;

  private C context;

  public DelegatingVisitor() {
    this(Filter1.TRUE);
  }

  public DelegatingVisitor(Filter1<Node> filter) {
    nodeFilter = newClassMap(Filter1Unsafe.class);
    this.filter = filter;
  }

  public DelegatingVisitor(VoidVisitor<C> delegate) {
    this();
    for (Method method : VoidVisitor.class.getMethods()) {
      if (method.getName().equals("visit")) {
        final Class<?> type = method.getParameterTypes()[0];
        nodeFilter.put(type, node->{
          method.invoke(delegate, node);
          return true;
        });
      }
    }
  }


  public C getContext() {
    return context;
  }

  public void setContext(C context) {
    this.context = context;
  }

  public DelegatingVisitor<T, C> withContext(C context) {
    setContext(context);
    return this;
  }

  public static class Context <T> {

    private T value;

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }

  }

  @Override
  public Boolean visit(
      CompilationUnit n, C ctx
  ) {
    if (checkNodeFilter(CompilationUnit.class, n)) {
      if (filter.filter(n)) {
        super.visit(n, ctx);
        return true;
      }
    }
    return false;
  }

  private boolean checkNodeFilter(Class<? extends Node> cls, Node n) {
    if (nodeFilter.containsKey(cls)) {
      return nodeFilter.get(cls).filter1(n);
    }
    return true;
  }

  @Override
  public Boolean visit(
      PackageDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ImportDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      TypeParameter n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      LineComment n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      BlockComment n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ClassOrInterfaceDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EnumDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EmptyTypeDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EnumConstantDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      AnnotationDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      AnnotationMemberDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      FieldDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      VariableDeclarator n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      VariableDeclaratorId n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ConstructorDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      MethodDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      Parameter n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      MultiTypeParameter n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EmptyMemberDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      InitializerDeclaration n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      JavadocComment n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ClassOrInterfaceType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      PrimitiveType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ReferenceType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      IntersectionType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      UnionType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      VoidType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      WildcardType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      UnknownType n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ArrayAccessExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ArrayCreationExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ArrayInitializerExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      AssignExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      BinaryExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      CastExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ClassExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ConditionalExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EnclosedExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      FieldAccessExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      InstanceOfExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      StringLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      TemplateLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      UiBodyExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      IntegerLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      LongLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      IntegerLiteralMinValueExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      LongLiteralMinValueExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      CharLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      DoubleLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      BooleanLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      NullLiteralExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      MethodCallExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      NameExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ObjectCreationExpr n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(QualifiedNameExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(ThisExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(SuperExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(UnaryExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(VariableDeclarationExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(MarkerAnnotationExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(SingleMemberAnnotationExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(NormalAnnotationExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(MemberValuePair n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(
      ExplicitConstructorInvocationStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      TypeDeclarationStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      AssertStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      BlockStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      LabeledStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      EmptyStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ExpressionStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      SwitchStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      SwitchEntryStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      BreakStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ReturnStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      IfStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      WhileStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ContinueStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      DoStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ForeachStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ForStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      ThrowStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      SynchronizedStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      TryStmt n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(
      CatchClause n, C ctx
  ) {
    return null;
  }

  @Override
  public Boolean visit(LambdaExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(MethodReferenceExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(TypeExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(UiAttrExpr n, C ctx) {
    return null;
  }

  @Override
  public Boolean visit(UiContainerExpr n, C ctx) {
    return null;
  }
}
