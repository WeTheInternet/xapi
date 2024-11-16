package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;
import xapi.string.X_String;

import static xapi.fu.In2Out1.superIn1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/17/16.
 */
public class ComposableXapiVisitor<Ctx> extends VoidVisitorAdapter<Ctx> {

    public static <Ctx, Generic extends Ctx> ComposableXapiVisitor<Ctx> onMissingIgnore(Class<Generic> logTo) {
        return whenMissingIgnore(logTo);
    }
    public static <Ctx> ComposableXapiVisitor<Ctx> whenMissingIgnore(Class<?> logTo) {
        final ComposableXapiVisitor<Ctx> visitor = new ComposableXapiVisitor<>(logTo);
        return visitor
            .withDefaultCallback((node, ctx, next)->{
                Log log = visitor.findLog(ctx, node);
                log.log(ComposableXapiVisitor.class, LogLevel.INFO,
                    "Ignoring unhandled node", node.getClass(), node, visitor);
                if (logTo != null) {
                    log.log(logTo, LogLevel.INFO, "<- visitor invoked from");
                }
            });
    }

    protected Log findLog(Object ... from) {
        return Log.firstLog(this, from);
    }

    public static <Ctx, Generic extends Ctx> ComposableXapiVisitor<Ctx> onMissingLog(Class<Generic> logTo, boolean visitAll) {
        return whenMissingLog(logTo, visitAll);
    }
    public static <Ctx> ComposableXapiVisitor<Ctx> whenMissingLog(Class<?> logTo, boolean visitAll) {
        final ComposableXapiVisitor<Ctx> visitor = new ComposableXapiVisitor<>(logTo);
        return visitor
            .withDefaultCallback((node, ctx, next)-> {
                Log log = visitor.findLog(ctx, node);
                log.log(ComposableXapiVisitor.class, LogLevel.INFO,
                    node, node.getClass(), " not handled by ", visitor);
                if (logTo != null) {
                    log.log(logTo, LogLevel.INFO, logTo.getName()," <- visitor invoked from");
                }
                if (visitAll) {
                    next.in(node, ctx);
                }
            });
    }

    public static <Ctx, Generic extends Ctx> ComposableXapiVisitor<Ctx> onMissingFail(Class<Generic> logTo) {
        return whenMissingFail(logTo);
    }

    public static <Ctx> ComposableXapiVisitor<Ctx> whenMissingFail(Class<?> logTo) {
        return whenMissingFail(logTo, Out1.EMPTY_STRING);
    }

    public static <Ctx> ComposableXapiVisitor<Ctx> whenMissingFail(Class<?> logTo, Out1<String> extraLog) {
        final ComposableXapiVisitor<Ctx> visitor = new ComposableXapiVisitor<>(logTo);
        return visitor
            .withDefaultCallback((node, ctx, next)-> {
                Log log = visitor.findLog(ctx, node);
                log.log(ComposableXapiVisitor.class, LogLevel.ERROR,
                    node, node.getClass(), " not handled by ", visitor);
                if (logTo != null) {
                    log.log(logTo, LogLevel.ERROR, "<- visitor invoked from");
                }
                String extra = extraLog.out1();
                if (X_String.isEmpty(extra)) {
                    // coerce null to ""
                    extra = "";
                } else {
                    // log user's extra log string
                    extra = extraLog + "\n";
                    log.log(ComposableXapiVisitor.class, LogLevel.ERROR, extra);
                }
                // Append user log string, if any, to exception message.
                throw new UnsupportedOperationException(extra + "Node " + node + " of type " + node.getClass() + " not handled by " + visitor);
            });
    }

    private MapLike<Class<?>, In2Out1<Node, Ctx, Boolean>> callbacks = X_Jdk.mapOrderedInsertion();
    private In3<Node, Ctx, In2<Node, Ctx>> defaultCallback = (n, c, i)->i.in(n, c);
    private final Object source;

    public ComposableXapiVisitor(){
        final StackTraceElement[] trace = X_Fu.currentStack();
        source = new Object() {
            @Override
            public String toString() {
                return X_String.join("\n\t", StackTraceElement::toString, trace);
            }
        };
    }

    public ComposableXapiVisitor(Object source){
        this.source = source;
    }

    private <N extends Node> void doVisit(Class<N> cls, N node, Ctx ctx, In2<N, Ctx> superCall) {
        In2Out1<Node, Ctx, Boolean> filter = callbacks.get(cls);
        boolean filterCheck;
        if (filter == null) {
            defaultCallback.in(node, ctx, (In2)superCall);
        } else if (filterCheck = filter.io(node, ctx)) {
            superCall.in(node, ctx);
//        } else { // The filter returned false; do nothing.
        }
    }

    public ComposableXapiVisitor<Ctx> withDefaultCallback(In3<Node, Ctx, In2<Node, Ctx>> callback) {
        this.defaultCallback = callback;
        return this;
    }

    protected <N extends Node> void putCallback(Class<N> cls, In2Out1<N, Ctx, Boolean> callback) {
        callbacks.put(cls, superIn1(callback));
    }

    @Override
    public void visit(AnnotationDeclaration n, Ctx arg) {
        doVisit(AnnotationDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withAnnotationDeclaration(In2Out1<AnnotationDeclaration, Ctx, Boolean> callback) {
        putCallback(AnnotationDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Ctx arg) {
        doVisit(AnnotationMemberDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withAnnotationMemberDeclaration(In2Out1<AnnotationMemberDeclaration, Ctx, Boolean> callback) {
        putCallback(AnnotationMemberDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(ArrayAccessExpr n, Ctx arg) {
        doVisit(ArrayAccessExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withArrayAccessExpr(In2Out1<ArrayAccessExpr, Ctx, Boolean> callback) {
        putCallback(ArrayAccessExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ArrayCreationExpr n, Ctx arg) {
        doVisit(ArrayCreationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withArrayCreationExpr(In2Out1<ArrayCreationExpr, Ctx, Boolean> callback) {
        putCallback(ArrayCreationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ArrayInitializerExpr n, Ctx arg) {
        doVisit(ArrayInitializerExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withArrayInitializerExpr(In2Out1<ArrayInitializerExpr, Ctx, Boolean> callback) {
        putCallback(ArrayInitializerExpr.class, callback);
        return this;
    }

    @Override
    public void visit(AssertStmt n, Ctx arg) {
        doVisit(AssertStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withAssertStmt(In2Out1<AssertStmt, Ctx, Boolean> callback) {
        putCallback(AssertStmt.class, callback);
        return this;
    }

    @Override
    public void visit(AssignExpr n, Ctx arg) {
        doVisit(AssignExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withAssignExpr(In2Out1<AssignExpr, Ctx, Boolean> callback) {
        putCallback(AssignExpr.class, callback);
        return this;
    }

    @Override
    public void visit(BinaryExpr n, Ctx arg) {
        doVisit(BinaryExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withBinaryExpr(In2Out1<BinaryExpr, Ctx, Boolean> callback) {
        putCallback(BinaryExpr.class, callback);
        return this;
    }

    @Override
    public void visit(BlockComment n, Ctx arg) {
        doVisit(BlockComment.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withBlockComment(In2Out1<BlockComment, Ctx, Boolean> callback) {
        putCallback(BlockComment.class, callback);
        return this;
    }

    @Override
    public void visit(BlockStmt n, Ctx arg) {
        doVisit(BlockStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withBlockStmt(In2Out1<BlockStmt, Ctx, Boolean> callback) {
        putCallback(BlockStmt.class, callback);
        return this;
    }

    @Override
    public void visit(BooleanLiteralExpr n, Ctx arg) {
        doVisit(BooleanLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withBooleanLiteralExpr(In2Out1<BooleanLiteralExpr, Ctx, Boolean> callback) {
        putCallback(BooleanLiteralExpr.class, callback);
        return this;
    }

    @Override
    public void visit(BreakStmt n, Ctx arg) {
        doVisit(BreakStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withBreakStmt(In2Out1<BreakStmt, Ctx, Boolean> callback) {
        putCallback(BreakStmt.class, callback);
        return this;
    }

    @Override
    public void visit(CastExpr n, Ctx arg) {
        doVisit(CastExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCastExpr(In2Out1<CastExpr, Ctx, Boolean> callback) {
        putCallback(CastExpr.class, callback);
        return this;
    }

    @Override
    public void visit(CatchClause n, Ctx arg) {
        doVisit(CatchClause.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCatchClause(In2Out1<CatchClause, Ctx, Boolean> callback) {
        putCallback(CatchClause.class, callback);
        return this;
    }

    @Override
    public void visit(CharLiteralExpr n, Ctx arg) {
        doVisit(CharLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCharLiteralExpr(In2Out1<CharLiteralExpr, Ctx, Boolean> callback) {
        putCallback(CharLiteralExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ClassExpr n, Ctx arg) {
        doVisit(ClassExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withClassExpr(In2Out1<ClassExpr, Ctx, Boolean> callback) {
        putCallback(ClassExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Ctx arg) {
        doVisit(ClassOrInterfaceDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withClassOrInterfaceDeclaration(In2Out1<ClassOrInterfaceDeclaration, Ctx, Boolean> callback) {
        putCallback(ClassOrInterfaceDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(ClassOrInterfaceType n, Ctx arg) {
        doVisit(ClassOrInterfaceType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withClassOrInterfaceType(In2Out1<ClassOrInterfaceType, Ctx, Boolean> callback) {
        putCallback(ClassOrInterfaceType.class, callback);
        return this;
    }

    @Override
    public void visit(CompilationUnit n, Ctx arg) {
        doVisit(CompilationUnit.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCompilationUnit(In2Out1<CompilationUnit, Ctx, Boolean> callback) {
        putCallback(CompilationUnit.class, callback);
        return this;
    }

    @Override
    public void visit(ConditionalExpr n, Ctx arg) {
        doVisit(ConditionalExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withConditionalExpr(In2Out1<ConditionalExpr, Ctx, Boolean> callback) {
        putCallback(ConditionalExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ConstructorDeclaration n, Ctx arg) {
        doVisit(ConstructorDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withConstructorDeclaration(In2Out1<ConstructorDeclaration, Ctx, Boolean> callback) {
        putCallback(ConstructorDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(ContinueStmt n, Ctx arg) {
        doVisit(ContinueStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withContinueStmt(In2Out1<ContinueStmt, Ctx, Boolean> callback) {
        putCallback(ContinueStmt.class, callback);
        return this;
    }

    @Override
    public void visit(DoStmt n, Ctx arg) {
        doVisit(DoStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withDoStmt(In2Out1<DoStmt, Ctx, Boolean> callback) {
        putCallback(DoStmt.class, callback);
        return this;
    }

    @Override
    public void visit(DoubleLiteralExpr n, Ctx arg) {
        doVisit(DoubleLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withDoubleLiteralExpr(In2Out1<DoubleLiteralExpr, Ctx, Boolean> callback) {
        putCallback(DoubleLiteralExpr.class, callback);
        return this;
    }

    @Override
    public void visit(EmptyMemberDeclaration n, Ctx arg) {
        doVisit(EmptyMemberDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEmptyMemberDeclaration(In2Out1<EmptyMemberDeclaration, Ctx, Boolean> callback) {
        putCallback(EmptyMemberDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(EmptyStmt n, Ctx arg) {
        doVisit(EmptyStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEmptyStmt(In2Out1<EmptyStmt, Ctx, Boolean> callback) {
        putCallback(EmptyStmt.class, callback);
        return this;
    }

    @Override
    public void visit(EmptyTypeDeclaration n, Ctx arg) {
        doVisit(EmptyTypeDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEmptyTypeDeclaration(In2Out1<EmptyTypeDeclaration, Ctx, Boolean> callback) {
        putCallback(EmptyTypeDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(EnclosedExpr n, Ctx arg) {
        doVisit(EnclosedExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEnclosedExpr(In2Out1<EnclosedExpr, Ctx, Boolean> callback) {
        putCallback(EnclosedExpr.class, callback);
        return this;
    }

    @Override
    public void visit(EnumConstantDeclaration n, Ctx arg) {
        doVisit(EnumConstantDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEnumConstantDeclaration(In2Out1<EnumConstantDeclaration, Ctx, Boolean> callback) {
        putCallback(EnumConstantDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(EnumDeclaration n, Ctx arg) {
        doVisit(EnumDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withEnumDeclaration(In2Out1<EnumDeclaration, Ctx, Boolean> callback) {
        putCallback(EnumDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Ctx arg) {
        doVisit(ExplicitConstructorInvocationStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withExplicitConstructorInvocationStmt(In2Out1<ExplicitConstructorInvocationStmt, Ctx, Boolean> callback) {
        putCallback(ExplicitConstructorInvocationStmt.class, callback);
        return this;
    }

    @Override
    public void visit(ExpressionStmt n, Ctx arg) {
        doVisit(ExpressionStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withExpressionStmt(In2Out1<ExpressionStmt, Ctx, Boolean> callback) {
        putCallback(ExpressionStmt.class, callback);
        return this;
    }

    @Override
    public void visit(FieldAccessExpr n, Ctx arg) {
        doVisit(FieldAccessExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withFieldAccessExpr(In2Out1<FieldAccessExpr, Ctx, Boolean> callback) {
        putCallback(FieldAccessExpr.class, callback);
        return this;
    }

    @Override
    public void visit(FieldDeclaration n, Ctx arg) {
        doVisit(FieldDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withFieldDeclaration(In2Out1<FieldDeclaration, Ctx, Boolean> callback) {
        putCallback(FieldDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(ForeachStmt n, Ctx arg) {
        doVisit(ForeachStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withForeachStmt(In2Out1<ForeachStmt, Ctx, Boolean> callback) {
        putCallback(ForeachStmt.class, callback);
        return this;
    }

    @Override
    public void visit(ForStmt n, Ctx arg) {
        doVisit(ForStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withForStmt(In2Out1<ForStmt, Ctx, Boolean> callback) {
        putCallback(ForStmt.class, callback);
        return this;
    }

    @Override
    public void visit(IfStmt n, Ctx arg) {
        doVisit(IfStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withIfStmt(In2Out1<IfStmt, Ctx, Boolean> callback) {
        putCallback(IfStmt.class, callback);
        return this;
    }

    @Override
    public void visit(ImportDeclaration n, Ctx arg) {
        doVisit(ImportDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withImportDeclaration(In2Out1<ImportDeclaration, Ctx, Boolean> callback) {
        putCallback(ImportDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(InitializerDeclaration n, Ctx arg) {
        doVisit(InitializerDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withInitializerDeclaration(In2Out1<InitializerDeclaration, Ctx, Boolean> callback) {
        putCallback(InitializerDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(InstanceOfExpr n, Ctx arg) {
        doVisit(InstanceOfExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withInstanceOfExpr(In2Out1<InstanceOfExpr, Ctx, Boolean> callback) {
        putCallback(InstanceOfExpr.class, callback);
        return this;
    }

    @Override
    public void visit(IntegerLiteralExpr n, Ctx arg) {
        doVisit(IntegerLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withIntegerLiteralExpr(In2Out1<IntegerLiteralExpr, Ctx, Boolean> callback) {
        putCallback(IntegerLiteralExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withIntegerLiteralTerminal(In2<IntegerLiteralExpr, Ctx> callback) {
        putCallback(IntegerLiteralExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(IntegerLiteralMinValueExpr n, Ctx arg) {
        doVisit(IntegerLiteralMinValueExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withIntegerLiteralMinValueExpr(In2Out1<IntegerLiteralMinValueExpr, Ctx, Boolean> callback) {
        putCallback(IntegerLiteralMinValueExpr.class, callback);
        return this;
    }

    @Override
    public void visit(JavadocComment n, Ctx arg) {
        doVisit(JavadocComment.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withJavadocComment(In2Out1<JavadocComment, Ctx, Boolean> callback) {
        putCallback(JavadocComment.class, callback);
        return this;
    }

    @Override
    public void visit(LabeledStmt n, Ctx arg) {
        doVisit(LabeledStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withLabeledStmt(In2Out1<LabeledStmt, Ctx, Boolean> callback) {
        putCallback(LabeledStmt.class, callback);
        return this;
    }

    @Override
    public void visit(LineComment n, Ctx arg) {
        doVisit(LineComment.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withLineComment(In2Out1<LineComment, Ctx, Boolean> callback) {
        putCallback(LineComment.class, callback);
        return this;
    }

    @Override
    public void visit(LongLiteralExpr n, Ctx arg) {
        doVisit(LongLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withLongLiteralExpr(In2Out1<LongLiteralExpr, Ctx, Boolean> callback) {
        putCallback(LongLiteralExpr.class, callback);
        return this;
    }

    @Override
    public void visit(LongLiteralMinValueExpr n, Ctx arg) {
        doVisit(LongLiteralMinValueExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withLongLiteralMinValueExpr(In2Out1<LongLiteralMinValueExpr, Ctx, Boolean> callback) {
        putCallback(LongLiteralMinValueExpr.class, callback);
        return this;
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Ctx arg) {
        doVisit(MarkerAnnotationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMarkerAnnotationExpr(In2Out1<MarkerAnnotationExpr, Ctx, Boolean> callback) {
        putCallback(MarkerAnnotationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(MemberValuePair n, Ctx arg) {
        doVisit(MemberValuePair.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMemberValuePair(In2Out1<MemberValuePair, Ctx, Boolean> callback) {
        putCallback(MemberValuePair.class, callback);
        return this;
    }

    @Override
    public void visit(MethodCallExpr n, Ctx arg) {
        doVisit(MethodCallExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMethodCallExpr(In2Out1<MethodCallExpr, Ctx, Boolean> callback) {
        putCallback(MethodCallExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withMethodCallExprTerminal(In2<MethodCallExpr, Ctx> callback) {
        return withMethodCallExpr(callback.supply1(false));
    }

    @Override
    public void visit(MethodDeclaration n, Ctx arg) {
        doVisit(MethodDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMethodDeclaration(In2Out1<MethodDeclaration, Ctx, Boolean> callback) {
        putCallback(MethodDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(NameExpr n, Ctx arg) {
        doVisit(NameExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withNameExpr(In2Out1<NameExpr, Ctx, Boolean> callback) {
        putCallback(NameExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withNameTerminal(In2<NameExpr, Ctx> callback) {
        putCallback(NameExpr.class, callback.supply1(false));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withNameOrStringOrType(In2<String, Ctx> callback) {
        return withNameOrString(callback).withTypeExprTerminal(callback.map1(TypeExpr::toString));
    }
    public ComposableXapiVisitor<Ctx> withNameOrString(In2<String, Ctx> callback) {
        return withNameTerminal((name, val) -> {
            String platName = name.getQualifiedName();
            callback.in(platName, val);
        })
        .withStringLiteralTerminal((name, val) -> {
            callback.in(name.getValue(), val);
        })
        .withTemplateLiteralTerminal((name, val) -> {
            callback.in(name.getValueWithoutTicks(), val);
        });
    }

    public ComposableXapiVisitor<Ctx> nameOrString(In1<String> callback) {
        return withNameOrString(callback.ignore2());
    }

    @Override
    public void visit(NormalAnnotationExpr n, Ctx arg) {
        doVisit(NormalAnnotationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withNormalAnnotationExpr(In2Out1<NormalAnnotationExpr, Ctx, Boolean> callback) {
        putCallback(NormalAnnotationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(NullLiteralExpr n, Ctx arg) {
        doVisit(NullLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withNullLiteralExpr(In2Out1<NullLiteralExpr, Ctx, Boolean> callback) {
        putCallback(NullLiteralExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ObjectCreationExpr n, Ctx arg) {
        doVisit(ObjectCreationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withObjectCreationExpr(In2Out1<ObjectCreationExpr, Ctx, Boolean> callback) {
        putCallback(ObjectCreationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(PackageDeclaration n, Ctx arg) {
        doVisit(PackageDeclaration.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withPackageDeclaration(In2Out1<PackageDeclaration, Ctx, Boolean> callback) {
        putCallback(PackageDeclaration.class, callback);
        return this;
    }

    @Override
    public void visit(Parameter n, Ctx arg) {
        doVisit(Parameter.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withParameter(In2Out1<Parameter, Ctx, Boolean> callback) {
        putCallback(Parameter.class, callback);
        return this;
    }

    @Override
    public void visit(MultiTypeParameter n, Ctx arg) {
        doVisit(MultiTypeParameter.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMultiTypeParameter(In2Out1<MultiTypeParameter, Ctx, Boolean> callback) {
        putCallback(MultiTypeParameter.class, callback);
        return this;
    }

    @Override
    public void visit(PrimitiveType n, Ctx arg) {
        doVisit(PrimitiveType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withPrimitiveType(In2Out1<PrimitiveType, Ctx, Boolean> callback) {
        putCallback(PrimitiveType.class, callback);
        return this;
    }

    @Override
    public void visit(QualifiedNameExpr n, Ctx arg) {
        doVisit(QualifiedNameExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withQualifiedNameExpr(In2Out1<QualifiedNameExpr, Ctx, Boolean> callback) {
        putCallback(QualifiedNameExpr.class, callback);
        return this;
    }

    public ComposableXapiVisitor<Ctx> withQualifiedNameTerminal(In2<QualifiedNameExpr, Ctx> callback) {
        putCallback(QualifiedNameExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(ReferenceType n, Ctx arg) {
        doVisit(ReferenceType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withReferenceType(In2Out1<ReferenceType, Ctx, Boolean> callback) {
        putCallback(ReferenceType.class, callback);
        return this;
    }

    @Override
    public void visit(IntersectionType n, Ctx arg) {
        doVisit(IntersectionType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withIntersectionType(In2Out1<IntersectionType, Ctx, Boolean> callback) {
        putCallback(IntersectionType.class, callback);
        return this;
    }

    @Override
    public void visit(UnionType n, Ctx arg) {
        doVisit(UnionType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUnionType(In2Out1<UnionType, Ctx, Boolean> callback) {
        putCallback(UnionType.class, callback);
        return this;
    }

    @Override
    public void visit(ReturnStmt n, Ctx arg) {
        doVisit(ReturnStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withReturnStmt(In2Out1<ReturnStmt, Ctx, Boolean> callback) {
        putCallback(ReturnStmt.class, callback);
        return this;
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Ctx arg) {
        doVisit(SingleMemberAnnotationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSingleMemberAnnotationExpr(In2Out1<SingleMemberAnnotationExpr, Ctx, Boolean> callback) {
        putCallback(SingleMemberAnnotationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(StringLiteralExpr n, Ctx arg) {
        doVisit(StringLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withStringLiteralExpr(In2Out1<StringLiteralExpr, Ctx, Boolean> callback) {
        putCallback(StringLiteralExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withStringLiteralTerminal(In2<StringLiteralExpr, Ctx> callback) {
        putCallback(StringLiteralExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(TemplateLiteralExpr n, Ctx arg) {
        doVisit(TemplateLiteralExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withTemplateLiteralExpr(In2Out1<TemplateLiteralExpr, Ctx, Boolean> callback) {
        putCallback(TemplateLiteralExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withTemplateLiteralTerminal(In2<TemplateLiteralExpr, Ctx> callback) {
        putCallback(TemplateLiteralExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(SuperExpr n, Ctx arg) {
        doVisit(SuperExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSuperExpr(In2Out1<SuperExpr, Ctx, Boolean> callback) {
        putCallback(SuperExpr.class, callback);
        return this;
    }

    @Override
    public void visit(SwitchEntryStmt n, Ctx arg) {
        doVisit(SwitchEntryStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSwitchEntryStmt(In2Out1<SwitchEntryStmt, Ctx, Boolean> callback) {
        putCallback(SwitchEntryStmt.class, callback);
        return this;
    }

    @Override
    public void visit(SwitchStmt n, Ctx arg) {
        doVisit(SwitchStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSwitchStmt(In2Out1<SwitchStmt, Ctx, Boolean> callback) {
        putCallback(SwitchStmt.class, callback);
        return this;
    }

    @Override
    public void visit(SynchronizedStmt n, Ctx arg) {
        doVisit(SynchronizedStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSynchronizedStmt(In2Out1<SynchronizedStmt, Ctx, Boolean> callback) {
        putCallback(SynchronizedStmt.class, callback);
        return this;
    }

    @Override
    public void visit(ThisExpr n, Ctx arg) {
        doVisit(ThisExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withThisExpr(In2Out1<ThisExpr, Ctx, Boolean> callback) {
        putCallback(ThisExpr.class, callback);
        return this;
    }

    @Override
    public void visit(ThrowStmt n, Ctx arg) {
        doVisit(ThrowStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withThrowStmt(In2Out1<ThrowStmt, Ctx, Boolean> callback) {
        putCallback(ThrowStmt.class, callback);
        return this;
    }

    @Override
    public void visit(TryStmt n, Ctx arg) {
        doVisit(TryStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withTryStmt(In2Out1<TryStmt, Ctx, Boolean> callback) {
        putCallback(TryStmt.class, callback);
        return this;
    }

    @Override
    public void visit(TypeDeclarationStmt n, Ctx arg) {
        doVisit(TypeDeclarationStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withTypeDeclarationStmt(In2Out1<TypeDeclarationStmt, Ctx, Boolean> callback) {
        putCallback(TypeDeclarationStmt.class, callback);
        return this;
    }

    @Override
    public void visit(TypeParameter n, Ctx arg) {
        doVisit(TypeParameter.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withTypeParameter(In2Out1<TypeParameter, Ctx, Boolean> callback) {
        putCallback(TypeParameter.class, callback);
        return this;
    }

    @Override
    public void visit(UnaryExpr n, Ctx arg) {
        doVisit(UnaryExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUnaryExpr(In2Out1<UnaryExpr, Ctx, Boolean> callback) {
        putCallback(UnaryExpr.class, callback);
        return this;
    }

    @Override
    public void visit(UnknownType n, Ctx arg) {
        doVisit(UnknownType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUnknownType(In2Out1<UnknownType, Ctx, Boolean> callback) {
        putCallback(UnknownType.class, callback);
        return this;
    }

    @Override
    public void visit(VariableDeclarationExpr n, Ctx arg) {
        doVisit(VariableDeclarationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withVariableDeclarationExpr(In2Out1<VariableDeclarationExpr, Ctx, Boolean> callback) {
        putCallback(VariableDeclarationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(VariableDeclarator n, Ctx arg) {
        doVisit(VariableDeclarator.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withVariableDeclarator(In2Out1<VariableDeclarator, Ctx, Boolean> callback) {
        putCallback(VariableDeclarator.class, callback);
        return this;
    }

    @Override
    public void visit(VariableDeclaratorId n, Ctx arg) {
        doVisit(VariableDeclaratorId.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withVariableDeclaratorId(In2Out1<VariableDeclaratorId, Ctx, Boolean> callback) {
        putCallback(VariableDeclaratorId.class, callback);
        return this;
    }

    @Override
    public void visit(VoidType n, Ctx arg) {
        doVisit(VoidType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withVoidType(In2Out1<VoidType, Ctx, Boolean> callback) {
        putCallback(VoidType.class, callback);
        return this;
    }

    @Override
    public void visit(WhileStmt n, Ctx arg) {
        doVisit(WhileStmt.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withWhileStmt(In2Out1<WhileStmt, Ctx, Boolean> callback) {
        putCallback(WhileStmt.class, callback);
        return this;
    }

    @Override
    public void visit(WildcardType n, Ctx arg) {
        doVisit(WildcardType.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withWildcardType(In2Out1<WildcardType, Ctx, Boolean> callback) {
        putCallback(WildcardType.class, callback);
        return this;
    }

    @Override
    public void visit(LambdaExpr n, Ctx arg) {
        doVisit(LambdaExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withLambdaExpr(In2Out1<LambdaExpr, Ctx, Boolean> callback) {
        putCallback(LambdaExpr.class, callback);
        return this;
    }

    @Override
    public void visit(MethodReferenceExpr n, Ctx arg) {
        doVisit(MethodReferenceExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withMethodReferenceExpr(In2Out1<MethodReferenceExpr, Ctx, Boolean> callback) {
        putCallback(MethodReferenceExpr.class, callback);
        return this;
    }
    public ComposableXapiVisitor<Ctx> withMethodReferenceRecurse(In2<MethodReferenceExpr, Ctx> callback) {
        putCallback(MethodReferenceExpr.class, callback.supply1(true));
        return this;
    }
    public ComposableXapiVisitor<Ctx> withMethodReferenceTerminal(In2<MethodReferenceExpr, Ctx> callback) {
        putCallback(MethodReferenceExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(TypeExpr n, Ctx arg) {
        doVisit(TypeExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withTypeExprTerminal(In2<TypeExpr, Ctx> callback) {
        putCallback(TypeExpr.class, callback.supply1(false));
        return this;
    }
    public ComposableXapiVisitor<Ctx> withTypeExpr(In2Out1<TypeExpr, Ctx, Boolean> callback) {
        putCallback(TypeExpr.class, callback);
        return this;
    }

    @Override
    public void visit(UiAttrExpr n, Ctx arg) {
        doVisit(UiAttrExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUiAttrExpr(In2Out1<UiAttrExpr, Ctx, Boolean> callback) {
        putCallback(UiAttrExpr.class, callback);
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiAttrTerminal(In2<UiAttrExpr, Ctx> callback) {
        putCallback(UiAttrExpr.class, callback.supply1(false));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiAttrRecurse(In1<UiAttrExpr> callback) {
        withUiAttrRecurse(callback.ignore2());
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiAttrRecurse(In2<UiAttrExpr, Ctx> callback) {
        putCallback(UiAttrExpr.class, callback.supply1(true));
        return this;
    }

    @Override
    public void visit(UiBodyExpr n, Ctx arg) {
        doVisit(UiBodyExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUiBodyExpr(In2Out1<UiBodyExpr, Ctx, Boolean> callback) {
        putCallback(UiBodyExpr.class, callback);
        return this;
    }

    @Override
    public void visit(UiContainerExpr n, Ctx arg) {
        doVisit(UiContainerExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withUiContainerExpr(In2Out1<UiContainerExpr, Ctx, Boolean> callback) {
        putCallback(UiContainerExpr.class, callback);
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiContainerExpr(In1Out1<UiContainerExpr, Boolean> callback) {
        putCallback(UiContainerExpr.class, callback.ignoresIn2());
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiContainerRecurse(In2<UiContainerExpr, Ctx> callback) {
        putCallback(UiContainerExpr.class, callback.supply1(true));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiContainerRecurse(In1<UiContainerExpr> callback) {
        putCallback(UiContainerExpr.class, callback.<Ctx>ignore2().supply1(true));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiContainerTerminal(In2<UiContainerExpr, Ctx> callback) {
        putCallback(UiContainerExpr.class, callback.supply1(false));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withUiContainerTerminal(In1<UiContainerExpr> callback) {
        putCallback(UiContainerExpr.class, callback.<Ctx>ignore2().supply1(false));
        return this;
    }

    @Override
    public void visit(JsonContainerExpr n, Ctx arg) {
        doVisit(JsonContainerExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withJsonContainerExpr(In2Out1<JsonContainerExpr, Ctx, Boolean> callback) {
        putCallback(JsonContainerExpr.class, callback);
        return this;
    }

    public ComposableXapiVisitor<Ctx> withJsonListRecurse() {
        return withJsonListOnly(Out1.immutable(true).ignoreIn2());
    }

    public ComposableXapiVisitor<Ctx> withJsonMapRecurse() {
        return withJsonListOnly(Out1.immutable(true).ignoreIn2());
    }
    public ComposableXapiVisitor<Ctx> withJsonListOnly(In2Out1<JsonContainerExpr, Ctx, Boolean> callback) {
        putCallback(JsonContainerExpr.class, callback.spyBefore((json, ctx)->{
            if (!json.isArray()) {
                throw new IllegalStateException("Expected only [json, array], got map: " + json.toSource());
            }
        }));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withJsonMapOnly(In2Out1<JsonContainerExpr, Ctx, Boolean> callback) {
        putCallback(JsonContainerExpr.class, callback.spyBefore((json, ctx)->{
            if (json.isArray()) {
                throw new IllegalStateException("Expected only {json: map}, got list: " + json.toSource());
            }
        }));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withJsonContainerRecurse(In2<JsonContainerExpr, Ctx> callback) {
        putCallback(JsonContainerExpr.class, callback.supply1(true));
        return this;
    }
    public ComposableXapiVisitor<Ctx> withJsonArrayRecurse(In2<JsonContainerExpr, Ctx> callback) {
        return withJsonArrayRecurse(callback, false);
    }

    public ComposableXapiVisitor<Ctx> withJsonArrayRecurse(In2<JsonContainerExpr, Ctx> callback, boolean allowFail) {
        putCallback(JsonContainerExpr.class, (json, ctx) -> {
            if (json.isArray()) {
                callback.in(json, ctx);
            } else if (!allowFail) {
                throw new IllegalArgumentException("Expected [array,] found illegal {json: map} " + json.toSource());
            }
            return true;
        });
        if (!callbacks.has(JsonPairExpr.class)) {
            withJsonPairTerminal((pair, ctx) -> pair.getValueExpr().accept(this, ctx));
        }
        return this;
    }
    public ComposableXapiVisitor<Ctx> withJsonContainerTerminal(In2<JsonContainerExpr, Ctx> callback) {
        putCallback(JsonContainerExpr.class, callback.supply1(false));
        return this;
    }

    @Override
    public void visit(JsonPairExpr n, Ctx arg) {
        doVisit(JsonPairExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withJsonPairExpr(In2Out1<JsonPairExpr, Ctx, Boolean> callback) {
        putCallback(JsonPairExpr.class, callback);
        return this;
    }

    public ComposableXapiVisitor<Ctx> withJsonPairTerminal(In2<JsonPairExpr, Ctx> callback) {
        putCallback(JsonPairExpr.class, callback.supply1(false));
        return this;
    }

    public ComposableXapiVisitor<Ctx> withJsonPairRecurse(In2<JsonPairExpr, Ctx> callback) {
        putCallback(JsonPairExpr.class, callback.supply1(true));
        return this;
    }

    @Override
    public void visit(CssBlockExpr n, Ctx arg) {
        doVisit(CssBlockExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCssBlockExpr(In2Out1<CssBlockExpr, Ctx, Boolean> callback) {
        putCallback(CssBlockExpr.class, callback);
        return this;
    }

    @Override
    public void visit(CssContainerExpr n, Ctx arg) {
        doVisit(CssContainerExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCssContainerExpr(In2Out1<CssContainerExpr, Ctx, Boolean> callback) {
        putCallback(CssContainerExpr.class, callback);
        return this;
    }

    @Override
    public void visit(CssRuleExpr n, Ctx arg) {
        doVisit(CssRuleExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCssRuleExpr(In2Out1<CssRuleExpr, Ctx, Boolean> callback) {
        putCallback(CssRuleExpr.class, callback);
        return this;
    }

    @Override
    public void visit(CssSelectorExpr n, Ctx arg) {
        doVisit(CssSelectorExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCssSelectorExpr(In2Out1<CssSelectorExpr, Ctx, Boolean> callback) {
        putCallback(CssSelectorExpr.class, callback);
        return this;
    }

    @Override
    public void visit(CssValueExpr n, Ctx arg) {
        doVisit(CssValueExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withDynamicDeclarationExpr(In2Out1<DynamicDeclarationExpr, Ctx, Boolean> callback) {
        putCallback(DynamicDeclarationExpr.class, callback);
        return this;
    }

    @Override
    public void visit(DynamicDeclarationExpr n, Ctx arg) {
        doVisit(DynamicDeclarationExpr.class, n, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withSysExpr(In2Out1<SysExpr, Ctx, Boolean> callback) {
        putCallback(SysExpr.class, callback);
        return this;
    }

    @Override
    public void visit(SysExpr sysExpr, Ctx arg) {
        doVisit(SysExpr.class, sysExpr, arg, super::visit);
    }

    public ComposableXapiVisitor<Ctx> withCssValueExpr(In2Out1<CssValueExpr, Ctx, Boolean> callback) {
        putCallback(CssValueExpr.class, callback);
        return this;
    }

    public SizedIterable<String> extractNames(Expression e, Ctx ctx) {
        ListLike<String> list = X_Jdk.list();
        extractNames(list::add);
        e.accept(this, ctx);
        return list;
    }
    public SizedIterable<String> extractNamesAndValuesAndTypes(Expression e, Ctx ctx) {
        ListLike<String> list = X_Jdk.list();
        extractNamesAndValuesAndTypes(list::add);
        e.accept(this, ctx);
        return list;
    }
    public SizedIterable<String> extractNamesAndValues(Expression e, Ctx ctx) {
        ListLike<String> list = X_Jdk.list();
        extractNamesAndValues(list::add);
        e.accept(this, ctx);
        return list;
    }

    public ComposableXapiVisitor<Ctx> extractNames(In1<String> onNameFound) {
        return withJsonArrayRecurse(In2.ignoreAll())
              .withNameOrString(onNameFound.ignore2());
    }

    public ComposableXapiVisitor<Ctx> extractNamesAndValuesAndTypes(In1<String> onNameFound) {
        return extractNamesAndValues(onNameFound)
                .withTypeExprTerminal(onNameFound.<TypeExpr>map1(TypeExpr::toSource).<Ctx>ignore2());
    }
    public ComposableXapiVisitor<Ctx> extractNamesAndValues(In1<String> onNameFound) {
        return withJsonArrayRecurse(In2.ignoreAll())
            .withBooleanLiteralExpr(onNameFound.<BooleanLiteralExpr>map1(BooleanLiteralExpr::toSource).<Ctx>ignore2().supply1(false))
            .withLongLiteralExpr(onNameFound.<LongLiteralExpr>map1(LongLiteralExpr::toSource).<Ctx>ignore2().supply1(false))
            .withIntegerLiteralExpr(onNameFound.<IntegerLiteralExpr>map1(IntegerLiteralExpr::toSource).<Ctx>ignore2().supply1(false))
            .withDoubleLiteralExpr(onNameFound.<DoubleLiteralExpr>map1(DoubleLiteralExpr::toSource).<Ctx>ignore2().supply1(false))
            .withCharLiteralExpr(onNameFound.<CharLiteralExpr>map1(CharLiteralExpr::toSource).<Ctx>ignore2().supply1(false))
            .withNameOrString(onNameFound.ignore2());
    }

    @Override
    public String toString() {
        return "ComposableXapiVisitor{" +
            "source=" + source +
            ", callbacks=" + callbacks +
            ", defaultCallback=" + defaultCallback +
            "} ";
    }
}
