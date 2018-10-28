package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WellKnownTypes;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import com.github.javaparser.ast.visitor.TransformVisitor;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.AstMethodInvoker.AstMethodResult;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.ImportSection;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.Filter.Filter1;
import xapi.fu.api.DoNotOverride;
import xapi.fu.itr.CountedIterator;
import xapi.fu.itr.MappedIterable;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.api.HasSourceBuilder;
import xapi.source.write.Template;
import xapi.util.X_String;

import java.util.ArrayList;
import java.util.List;

import static com.github.javaparser.ast.expr.BooleanLiteralExpr.boolLiteral;
import static com.github.javaparser.ast.expr.CharLiteralExpr.charLiteral;
import static com.github.javaparser.ast.expr.DoubleLiteralExpr.doubleLiteral;
import static com.github.javaparser.ast.expr.IntegerLiteralExpr.intLiteral;
import static com.github.javaparser.ast.expr.LongLiteralExpr.longLiteral;
import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/22/16.
 */
public interface ApiGeneratorTools <Ctx extends ApiGeneratorContext<Ctx>> extends Rethrowable {

    default void addTypeParams(Ctx ctx, SourceBuilder<?> builder, Expression typeParams) {
        resolveToLiterals(ctx, typeParams)
            .forEachValue(builder.getClassBuffer()::addGenerics);
    }

    default String resolveLiteral(Ctx ctx, Expression node) {
        final IntTo<String> literals = resolveToLiterals(ctx, node);
        if (literals.size() != 1) {
            throw new IllegalArgumentException("Expecting only a single literal result from " + debugNode(node) +";" +
                "\nInstead received: " + literals);
        }
        return literals.at(0);
    }

    default ApiGeneratorMethods methods() {
        return ()->this;
    }

    default IntTo<String> resolveToLiterals(Ctx ctx, Expression typeParams) {
        return resolveToLiterals(ctx, typeParams, false);
    }
    default IntTo<String> resolveToLiterals(Ctx ctx, Expression typeParams, boolean allowMisses) {
        IntTo<String> types = X_Collect.newList(String.class);
        ComposableXapiVisitor<Ctx> resolver = new ComposableXapiVisitor<>();
        resolver
            .withDefaultCallback((node, context, superCall)->{
                if (!allowMisses) {
                    throw new NotYetImplemented("No handler registered for " + debugNode(node));
                }
                final String sourced = node.toSource(getTransformer(ctx));
                types.add(sourced);
            })
            .withEnclosedExpr((node, context)->{
                node.getInner().accept(resolver, context);
                return false;
            })
            .withInstanceOfExpr((node, context)->{
                types.add(node.toSource());
                return false;
            })
            .withMethodCallExpr((call, c)->{
                Maybe<AstMethodInvoker<Ctx>> toInvoke = methods().findMethod(ctx, call);
                if (toInvoke.isPresent()) {
                    final AstMethodResult res = toInvoke.get().io(this, ctx, call);
                    if (res.getActualResult() == call) {
                        X_Log.warn(getClass(), "Invoker failure; expect errors", toInvoke.get(), res);
                    } else {
                        if (res.getActualResult() != null) {
                            res.getExprs().forEach(expr->expr.accept(resolver, ctx));
                            return false;
                        }
                    }
                }
                // No magic methods; just turn the method call into a template and return a string...
                MethodCallExpr copy = (MethodCallExpr) call.clone();
                final Expression[] args = copy.getArgs().toArray(new Expression[0]);
                copy.getArgs().clear();
                for (Expression arg : args) {
                    final Expression resolvedArg = resolveVar(ctx, arg);
                    // Maybe expand this expression?
                    if (Boolean.TRUE.equals(resolvedArg.getExtra("isWrappedVararg"))) {
                        JsonContainerExpr list = (JsonContainerExpr) resolvedArg;
                        list.getPairs().stream()
                            .map(JsonPairExpr::getValueExpr)
                            .map(ex-> resolveVar(ctx, ex))
                            .forEach(copy.getArgs()::add);
                    } else {
                        copy.getArgs().add(resolvedArg);
                    }
                }

                final Transformer LITERAL_TRANSFORMER = new Transformer(){
                    @Override
                    public void normalizeToString(Printable printer, String template) {
                        printer.print(template);
                    }
                };
                types.add(resolveTemplate(ctx, templateLiteral(copy.toSource(LITERAL_TRANSFORMER))));
                return false;
            })
            .withSysExpr((expr, c)->{
                final In1<Node> callback = expr.voidVisit(resolver, c)
                    .provide1(resolver)
                    .provide2(c);
                expr.readAllNodes(callback, ctx);
                return false;
            })
            .withUiContainerExpr((ui, c)-> {
                String src = ui.toSource(getTransformer(c));
                final String[] lines = TransformVisitor.normalizeLines(src);
                types.add(X_String.join("\n", lines));
                return false;
            })
            .withFieldAccessExpr((fa, c)->{
                String result = resolveTemplate(ctx, templateLiteral(fa.toSource()));
                types.add(result);
                return false;
            })
            .withTypeExpr((expr, c)->{
                String result = resolveTemplate(ctx, templateLiteral(expr.getType().toSource()));
                types.add(result);
                return false;
            })
            .withTemplateLiteralExpr((expr, c)->{
                String result = resolveTemplate(ctx, expr);
                types.add(result);
                return false;
            })
            .withJsonContainerExpr((json, c)->{
                if (json.isArray()) {
                    json.getPairs().forEach(pair->{
                        final IntTo<String> result = resolveToLiterals(ctx, pair.getValueExpr());
                        types.addAll(result);
                    });
                } else {
                    json.getPairs().forEach(pair -> {
                        final Expression resolved = resolveVar(ctx, pair.getValueExpr());
                        final IntTo<String> paramType = resolveToLiterals(ctx, resolved);
                        if (paramType.size() != 1) {
                            throw new IllegalArgumentException("Parameter types using json notation must have " +
                                "values which return exactly one type;" +
                                "\nyou sent " + debugNode(pair.getValueExpr()) + "" +
                                "\nwhich returned " + paramType +
                                "\nfrom " + debugNode(json));
                        }
                        final String name = resolveString(ctx, templateLiteral(pair.getKeyString()));
                        types.add(paramType.get(0) + " " + name);
                    });
                }
                return false;
            })
            .withStringLiteralExpr((expr, c)->types.add(expr.getValue()))
            .withBooleanLiteralExpr((expr, c)->types.add(Boolean.toString(expr.getValue())))
            .withIntegerLiteralExpr((expr, c)->types.add(String.valueOf(expr.getValue())))
            .withIntegerLiteralMinValueExpr((expr, c)->types.add(String.valueOf(expr.getValue())))
            .withDoubleLiteralExpr((expr, c)->types.add(String.valueOf(expr.getValue())))
            .withLongLiteralExpr((expr, c)->types.add(String.valueOf(expr.getValue())))
            .withCharLiteralExpr((expr, c)->types.add(String.valueOf(expr.getValue())))
            .withQualifiedNameExpr((expr, c)-> {
                if ("class".equals(expr.getSimpleName())) {
                    expr.getQualifier().accept(resolver, c);
                    return false;
                }
                final Expression scope = expr.getRootScope();
                if (scope instanceof NameExpr && ((NameExpr)scope).getSimpleName().startsWith("$")) {
                    // when a qualified name starts with a $ref, we want to descend into that name...
                    return true;
                }
                // Serialize this name and finish
                types.add(resolveTemplate(c, templateLiteral(expr.getQualifiedName())));
                return false;
            })
            .withNameExpr((expr, c)->{
                String name = expr.getName();
                if (name.contains("$")) {
                    types.add(resolveTemplate(c, templateLiteral(name)));
                } else {
                    types.add(name);
                }
                return false;
            })
            .withBinaryExpr((expr, c)->{
                final Expression result = resolveVar(ctx, expr);
                result.accept(resolver, c);
                return false;
            })
            .withConditionalExpr((expr, c)->{
                final Expression evaled = resolveVar(c, expr.getCondition());
                if (isConditionTrue(ctx, evaled)) {
                    expr.getThenExpr().accept(resolver, c);
                } else {
                    expr.getElseExpr().accept(resolver, c);
                }
                return false;
            })
            .withArrayInitializerExpr((expr, c)->{
                expr.getValues().forEach(value->
                    types.addAll(resolveToLiterals(ctx, value))
                );
                return false;
            })
            .withClassExpr((expr, c)->{

                Type type = expr.getType();
                int arrayCnt = 0;
                if (type instanceof ReferenceType) {
                    arrayCnt = ((ReferenceType)type).getArrayCount();
                    type = ((ReferenceType)type).getType();
                }
                String arrays = X_String.repeat("[]", arrayCnt);
                if (type instanceof VoidType) {
                    types.add("void");
                    return false;
                } else if (type instanceof ClassOrInterfaceType) {
                    types.add(((ClassOrInterfaceType)type).getName() + arrays);
                    return false;
                } else {
                    throw new IllegalArgumentException("Unhandled ClassExpr type " + type + " from " + expr + " at " + expr.getCoordinates());
                }
            });
        typeParams.accept(resolver, ctx);
        return types;
    }

    default Transformer getTransformer(Ctx ctx) {
        return new Transformer() {
            @Override
            public String onTemplateStart(Printable printer, TemplateLiteralExpr template) {
                return resolveString(ctx, template);
            }

            @Override
            public String resolveName(Printable printer, NameExpr name) {
                return resolveString(ctx, name);
            }

            @Override
            public String resolveType(ClassOrInterfaceType type) {
                return resolveString(ctx, templateLiteral(type.getName()));
            }

            @Override
            public String resolveTypeParamName(TypeParameter typeParam) {
                if (ctx.hasNode(typeParam.getName())) {
                    final IntTo<String> literals = resolveToLiterals(
                        ctx,
                        (Expression) ctx.getNode(typeParam.getName())
                    );
                    return literals.join(", ");
                }
                return resolveString(ctx, templateLiteral(typeParam.toSource()));
            }

            @Override
            public void onTemplateEnd(Printable printer) {
            }
        };
    }

    default String debugNode(Node node) {
        return node == null ? "null" : node + " of type " + node.getClass() + " at " + node.getCoordinates();
    }

    @DoNotOverride
    default String resolveStringReverse(Expression value, Ctx ctx) {
        return resolveString(ctx, value);
    }
    @DoNotOverride
    default Expression resolveVarReverse(Expression value, Ctx ctx) {
        return resolveVar(ctx, value);
    }
    default String resolveString(Ctx ctx, Expression value) {
        return resolveString(ctx, value, false);
    }
    default String resolveString(Ctx ctx, Expression value, boolean allowEmpty) {
        return resolveString(ctx, value, allowEmpty, false);
    }
    default String resolveString(Ctx ctx, Expression value, boolean allowEmpty, boolean allowMisses) {
        final IntTo<String> literals = resolveToLiterals(ctx, value, allowMisses);
        if (literals.size() != 1) {
            if (allowEmpty && literals.isEmpty()) {
                return "";
            }
            throw new IllegalStateException("Cannot extract a single String argument for " + debugNode(value)
              + "\nThis node resulted in: " + literals);
        }
        return literals.get(0);
//
//        if (value instanceof TemplateLiteralExpr) {
//            return resolveTemplate(ctx, (TemplateLiteralExpr) value);
//        } else if (value instanceof StringLiteralExpr) {
//            return ((StringLiteralExpr)value).getValue();
//        } else if (value instanceof BooleanLiteralExpr) {
//            return Boolean.toString(((BooleanLiteralExpr)value).getValue());
//        } else if (value instanceof NameExpr) {
//            String name = ((NameExpr)value).getName();
//            if (name.startsWith("$")) {
//                return ctx.getString(name);
//            } else {
//                return name;
//            }
//        } else {
//            throw new IllegalArgumentException("Unable to extract string from " + value.getClass() + " : " + value + " @ " + value.getCoordinates());
//        }
    }
    default String resolveTemplate(Ctx ctx, TemplateLiteralExpr value) {
        return ctx.resolveValues(value.getValueWithoutTicks(), item->resolveToLiterals(ctx, (Expression)item).join(", "));
    }

    default List<String> resolveRange(Ctx ctx, MethodCallExpr call) {
        final List<String> resolved = new ArrayList<>();
        final List<Expression> args = call.getArgs();

        if (args.size() != 3 && args.size() != 4) {
            throw new IllegalArgumentException("$range lambdas must " +
                "have exactly three or four arguments; you sent " + args + " via " + debugNode(call));
        }
        int start = extractInt(ctx, args.get(0));
        int end = extractInt(ctx, args.get(1));
        Filter1<Integer> filter;
        final Expression result;
        if (args.size() == 3) {
            filter = always->true;
            result = args.get(2);
        } else {
            final Expression filterExpr = args.get(2);
            if (!(filterExpr instanceof LambdaExpr)) {
                throw new IllegalArgumentException("When using the 4-arg $range method, the third " +
                    "argument must be a lambda (for filtering items out of the range); you sent " + debugNode(call));
            }
            LambdaExpr filterLambda = (LambdaExpr) filterExpr;
            if (filterLambda.getParameters().size() != 1) {
                throw new IllegalArgumentException("$range lambdas must have " +
                    "exactly one argument; you sent " + debugNode(filterLambda));
            }
            final Statement filterBody = filterLambda.getBody();
            if (!(filterBody instanceof ExpressionStmt)) {
                throw new IllegalArgumentException("$range filter lambdas must have " +
                    "a single expression body (no braces), you sent " + debugNode(filterLambda));
            }
            final Expression filterExpression = ((ExpressionStmt) filterBody).getExpression();
            String filterName = filterLambda.getParameters().get(0).getId().getName();
            filter = i->{
                Do undo = ctx.addToContext(filterName, intLiteral(i));
                boolean allowed = isConditionTrue(ctx, filterExpression);
                undo.done();
                return allowed;
            };
            result = args.get(3);
        }
        if (result instanceof LambdaExpr) {
            LambdaExpr lambda = (LambdaExpr) result;
            if (lambda.getParameters().size() != 1) {
                throw new IllegalArgumentException("$range lambdas must have " +
                    "exactly one argument; you sent " + lambda + " at " + lambda.getCoordinates());
            }
            String varName = lambda.getParameters().get(0).getId().getName();
            boolean first = true;
            ctx.setInRange(true);
            for (; start <= end; start ++ ) {
                if (filter.filter1(start)) {
                    ctx.setFirstOfRange(first);
                    if (first) {
                        first = false;
                    }
                    final Do undo = ctx.addToContext(varName, intLiteral(start));
                    try {
                        if (lambda.getBody() instanceof ExpressionStmt) {
                            // A lambda with a single expression for a body
                            final ExpressionStmt body = (ExpressionStmt) lambda.getBody();
                            if (body.getExpression() instanceof TemplateLiteralExpr) {
                                String templateString = ASTHelper.extractStringValue(body.getExpression());
                                Template template = new Template(templateString, varName);
                                resolved.add(template.apply(start));
                            } else {
                                // A more complex body... we'll want to recurse...
                                resolved.add(resolveBody(ctx, body.getExpression()));
                            }
                        } else if (lambda.getBody() instanceof BlockStmt) {
                            // A lambda with a block of expressions for its body.
                            throw new NotYetImplemented("Lambdas with block expressions not yet supported " + debugNode(lambda));
                        } else {
                            throw new IllegalArgumentException("Unable to process lambda body " + debugNode(lambda.getBody()));
                        }
                    } finally {
                        undo.done();
                    }
                }
            }
            ctx.setInRange(false);
        }
        return resolved;
    }

    default int extractInt(Ctx ctx, Expression parameter) {
        final Expression resolved = resolveVar(ctx, parameter);
        if (resolved instanceof StringLiteralExpr) {
            return Integer.parseInt(ASTHelper.extractStringValue(resolved));
        } else if (resolved instanceof NameExpr) {
            String name = ((NameExpr)resolved).getName();
            // Now that we have the variable name, lets pull it from context
            int size = Integer.parseInt(ctx.getString(name));
            return size;
        }
        throw new IllegalArgumentException("Cannot extract int from " + debugNode(parameter) +
            (resolved == parameter ? "" : "; resolved to: " + debugNode(resolved)));
    }

    default Expression resolveNode(Ctx ctx, Node node) {
        if (node instanceof Expression) {
            return resolveVar(ctx, (Expression) node);
        } else if (node instanceof ExpressionStmt) {
            return resolveVar(ctx, ((ExpressionStmt)node).getExpression());
        } else if (node instanceof ReturnStmt) {
            return resolveVar(ctx, ((ReturnStmt)node).getExpr());
        } else {
            throw new IllegalArgumentException("Unable to resolve node " + debugNode(node));
        }
    }

    default JsonContainerExpr resolveMethod(Ctx ctx, MethodCallExpr methodCall) {
        final List<JsonPairExpr> pairs = new ArrayList<>();
        if (methodCall.getArgs().size() == 3) {
            // format is returnType, name, params
            pairs.add(new JsonPairExpr("returns", methodCall.getArg(0)));
            pairs.add(new JsonPairExpr("name", methodCall.getArg(1)));
            pairs.add(new JsonPairExpr("params", methodCall.getArg(2)));
        } else if (methodCall.getArgs().size() == 4) {
            // is either: [types], returnType, name, params
            // or: returnType, name, params, body
            if (methodCall.getArg(0) instanceof JsonContainerExpr) {
                // no body
                pairs.add(new JsonPairExpr("typeParams", methodCall.getArg(0)));
                pairs.add(new JsonPairExpr("returns", methodCall.getArg(1)));
                pairs.add(new JsonPairExpr("name", methodCall.getArg(2)));
                pairs.add(new JsonPairExpr("params", methodCall.getArg(3)));
            } else {
                // no type params
                pairs.add(new JsonPairExpr("returns", methodCall.getArg(0)));
                pairs.add(new JsonPairExpr("name", methodCall.getArg(1)));
                pairs.add(new JsonPairExpr("params", methodCall.getArg(2)));
                pairs.add(new JsonPairExpr("body", methodCall.getArg(3)));
            }
        } else if (methodCall.getArgs().size() == 5) {
            // format is [types], returnType, name, params, body
            pairs.add(new JsonPairExpr("typeParams", methodCall.getArg(0)));
            pairs.add(new JsonPairExpr("returns", methodCall.getArg(1)));
            pairs.add(new JsonPairExpr("name", methodCall.getArg(2)));
            pairs.add(new JsonPairExpr("params", methodCall.getArg(3)));
            pairs.add(new JsonPairExpr("body", methodCall.getArg(4)));
        } else {
            throw new IllegalArgumentException("Malformed method() definition " + methodCall + " at " + methodCall.getCoordinates());
        }
        return new JsonContainerExpr(false, pairs);
    }

    default String resolveBody(Ctx ctx, Expression valueExpr) {
        StringBuilder b = new StringBuilder();
        resolveExpressions(ctx, b, valueExpr);
        return b.toString();
    }

    default void resolveExpressions(Ctx ctx, StringBuilder b, Expression valueExpr) {
        if (valueExpr instanceof EnclosedExpr) {
            resolveExpressions(ctx, b, ((EnclosedExpr)valueExpr).getInner());
        } else if (valueExpr instanceof BinaryExpr) {
            BinaryExpr expr = (BinaryExpr) valueExpr;
            switch (expr.getOperator()) {
                case plus:
                    // append left then append right
                    resolveExpressions(ctx, b, expr.getLeft());
                    resolveExpressions(ctx, b, expr.getRight());
                    break;
                default:
                    final Expression resolved = resolveVar(ctx, valueExpr);
                    if (resolved == valueExpr) {
                        throw new IllegalArgumentException("Unhandled binary operator " + expr.getOperator() + " in " + debugNode(valueExpr));
                    }
                    resolveExpressions(ctx, b, resolved);
            }
        } else if (valueExpr instanceof ConditionalExpr) {
            ConditionalExpr conditional = (ConditionalExpr) valueExpr;
            final Expression condition = conditional.getCondition();
            if (isConditionTrue(ctx, condition)) {
                resolveExpressions(ctx, b, conditional.getThenExpr());
            } else {
                resolveExpressions(ctx, b, conditional.getElseExpr());
            }
        } else if (valueExpr instanceof TemplateLiteralExpr) {
            b.append(resolveString(ctx, valueExpr));
        } else if (valueExpr instanceof StringLiteralExpr) {
            b.append(((StringLiteralExpr)valueExpr).getValue());
        } else if (valueExpr instanceof MethodCallExpr) {
            final IntTo<String> literals = resolveToLiterals(ctx, valueExpr);
            literals.forEachValue(b::append);
        } else {
            throw new IllegalArgumentException("Cannot resolve expression " + debugNode(valueExpr));
        }
    }

    default boolean isConditionTrue(Ctx ctx, Statement condition) {
        if (condition instanceof ExpressionStmt) {
            return isConditionTrue(ctx, ((ExpressionStmt)condition).getExpression());
        } else if (condition instanceof ReturnStmt) {
            return isConditionTrue(ctx, ((ReturnStmt)condition).getExpr());
        } else {
            throw new IllegalArgumentException("Unhandled statement type " + debugNode(condition));
        }
    }
    default boolean isConditionTrue(Ctx ctx, Expression condition) {
        if (condition instanceof BinaryExpr) {
            BinaryExpr expr = (BinaryExpr) condition;
            final Expression left = resolveVar(ctx, expr.getLeft());
            final Expression right = resolveVar(ctx, expr.getRight());
            String leftValue = resolveString(ctx, left);
            String rightValue = resolveString(ctx, right);
            switch(expr.getOperator()) {
                case equals:
                    return leftValue.equals(rightValue);
                case notEquals:
                    return !leftValue.equals(rightValue);
                case less:
                    return Double.parseDouble(leftValue) < Double.parseDouble(rightValue);
                case lessEquals:
                    return Double.parseDouble(leftValue) <= Double.parseDouble(rightValue);
                case greater:
                    return Double.parseDouble(leftValue) > Double.parseDouble(rightValue);
                case greaterEquals:
                    return Double.parseDouble(leftValue) >= Double.parseDouble(rightValue);
                case and:
                    return Boolean.parseBoolean(leftValue) & Boolean.parseBoolean(rightValue);
                case or:
                    return Boolean.parseBoolean(leftValue) | Boolean.parseBoolean(rightValue);
                case xor:
                    return Boolean.parseBoolean(leftValue) ^ Boolean.parseBoolean(rightValue);
                default:
                    throw new IllegalArgumentException("Unsupported binary operator in conditional expression; " + debugNode(condition));
            }
        } else if (condition instanceof MethodCallExpr) {
            MethodCallExpr expr = (MethodCallExpr) condition;
            switch (expr.getName()) {
                case "$if":
                case "$elseIf":
                    return isConditionTrue(ctx, expr.getArg(0));
                default:
                    throw new IllegalArgumentException("Cannot extract conditional truth from method call: " + debugNode(expr));
            }
        } else if (condition instanceof EnclosedExpr) {
            return isConditionTrue(ctx, ((EnclosedExpr)condition).getInner());
        }
        return "true".equals(resolveString(ctx, condition));
    }

    default int resolveInt(Ctx ctx, Expression filter) {
        final Expression result = resolveVar(ctx, filter);
        if (result instanceof IntegerLiteralExpr) {
            final String value = ((IntegerLiteralExpr) result).getValue();
            return Integer.parseInt(value);
        } else {
            String asString = resolveString(ctx, result);
            return Integer.parseInt(asString);
        }
    }
    default Expression resolveVar(Ctx ctx, Expression valueExpr) {
        if (valueExpr instanceof EnclosedExpr) {
            return resolveVar(ctx, ((EnclosedExpr)valueExpr).getInner());
        } else if (valueExpr instanceof BinaryExpr) {
            BinaryExpr expr = (BinaryExpr)valueExpr;
            Expression left = (Expression) lookupNode(ctx, expr.getLeft());
            Expression right = (Expression) lookupNode(ctx, expr.getRight());
            left = resolveVar(ctx, left);
            right = resolveVar(ctx, right);
            switch (expr.getOperator()) {
                // any in, any out...
                case plus:
                    if (arePrimitiveLiterals(ctx, left, right)) {
                      // numeric types should preserve their type after addition
                      if (areIntTypes(ctx, left, right)) {
                          Integer leftI = ((IntegerLiteralExpr)left).intValue();
                          Integer rightI = ((IntegerLiteralExpr)right).intValue();
                          return intLiteral(leftI + rightI);
                      } else if (areDoubleTypes(ctx, left, right)) {
                          Double leftD = ((DoubleLiteralExpr)left).doubleValue();
                          Double rightD = ((DoubleLiteralExpr)right).doubleValue();
                          return doubleLiteral(leftD + rightD);
                      } else if (areLongTypes(ctx, left, right)) {
                          Long leftL = ((LongLiteralExpr)left).longValue();
                          Long rightL = ((LongLiteralExpr)right).longValue();
                          return longLiteral(leftL + rightL);
                      } else if (areCharTypes(ctx, left, right)) {
                          Character leftL = ((CharLiteralExpr)left).charValue();
                          Character rightL = ((CharLiteralExpr)right).charValue();
                          return charLiteral((char)((int)leftL.charValue() + (int)rightL.charValue()));
                      } else {
                          throw new UnsupportedOperationException("Cannot add the nodes of " + debugNode(expr));
                      }
                    } else {
                      // string concat for anything else...
                      String leftString = resolveString(ctx, expr.getLeft());
                      String rightString = resolveString(ctx, expr.getRight());
                      return new StringLiteralExpr(leftString + rightString);
                    }
                // numeric in, numeric out
                case minus:
                case times:
                case divide:
                case remainder:
                case lShift:
                case rSignedShift:
                case rUnsignedShift:
                case xor:
                case binAnd:
                case binOr:
                    if (areIntTypes(ctx, left, right)) {
                        Integer leftI = ((IntegerLiteralExpr)left).intValue();
                        Integer rightI = ((IntegerLiteralExpr)right).intValue();
                        int result = apply(expr.getOperator(), leftI, rightI);
                        return intLiteral(result);
                    } else if (areDoubleTypes(ctx, left, right)) {
                        Double leftD = ((DoubleLiteralExpr)left).doubleValue();
                        Double rightD = ((DoubleLiteralExpr)right).doubleValue();
                        double result = apply(expr.getOperator(), leftD, rightD);
                        return doubleLiteral(result);
                    } else if (areLongTypes(ctx, left, right)) {
                        Long leftL = ((LongLiteralExpr) left).longValue();
                        Long rightL = ((LongLiteralExpr) right).longValue();
                        long result = apply(expr.getOperator(), leftL, rightL);
                        return longLiteral(result);
                    } else if (areCharTypes(ctx, left, right)) {
                        Character leftC = ((CharLiteralExpr)left).charValue();
                        Character rightC = ((CharLiteralExpr)right).charValue();
                        char result = apply(expr.getOperator(), leftC, rightC);
                        return charLiteral(result);
                    } else {
                        throw new UnsupportedOperationException("Cannot " + expr.getOperator() + " the nodes of " + debugNode(expr) +
                        "\nLeft: " + debugNode(left) + ", Right: " + debugNode(right));
                    }
                // numeric in, boolean out
                case greater:
                case greaterEquals:
                case less:
                case lessEquals:
                    if (areIntTypes(ctx, left, right)) {
                        Integer leftI = ((IntegerLiteralExpr)left).intValue();
                        Integer rightI = ((IntegerLiteralExpr)right).intValue();
                        return boolLiteral(check(expr.getOperator(), leftI, rightI));
                    } else if (areDoubleTypes(ctx, left, right)) {
                        Double leftD = ((DoubleLiteralExpr)left).doubleValue();
                        Double rightD = ((DoubleLiteralExpr)right).doubleValue();
                        return boolLiteral(check(expr.getOperator(), leftD, rightD));
                    } else if (areLongTypes(ctx, left, right)) {
                        Long leftL = ((LongLiteralExpr) left).longValue();
                        Long rightL = ((LongLiteralExpr) right).longValue();
                        return boolLiteral(check(expr.getOperator(), leftL, rightL));
                    } else if (areCharTypes(ctx, left, right)) {
                        Character leftC = ((CharLiteralExpr)left).charValue();
                        Character rightC = ((CharLiteralExpr)right).charValue();
                        return boolLiteral(check(expr.getOperator(), (int)leftC.charValue(), (int)rightC.charValue()));
                    } else {
                        throw new UnsupportedOperationException("Cannot " + expr.getOperator() + " the nodes of " + debugNode(expr) +
                            "\nLeft: " + debugNode(left) + ", Right: " + debugNode(right));
                    }
                // boolean in, boolean out
                case and:
                case or:
                    if (!(left instanceof BooleanLiteralExpr)) {
                        throw new IllegalArgumentException("Left side of " + debugNode(expr)
                        +" did not evaluate to a BooleanLiteral;\nreceived: " + debugNode(left));
                    }
                    if (!(right instanceof BooleanLiteralExpr)) {
                        throw new IllegalArgumentException("Right side of " + debugNode(expr)
                        +" did not evaluate to a BooleanLiteral;\nreceived: " + debugNode(right));
                    }
                    boolean l = ((BooleanLiteralExpr)left).getValue();
                    boolean r = ((BooleanLiteralExpr)right).getValue();
                    if (expr.getOperator() == Operator.and) {
                        return boolLiteral(l && r);
                    } else if (expr.getOperator() == Operator.or) {
                        return boolLiteral(l && r);
                    } else {
                        throw new IllegalStateException("Can't get here; bad node: " + debugNode(expr));
                    }
                // any type; convert to then compare string is easiest
                case equals:
                case notEquals:
                    left = resolveVar(ctx, left);
                    right = resolveVar(ctx, right);
                    final String leftString = resolveString(ctx, left);
                    if (leftString == null) {
                        throw new IllegalArgumentException("Invalid left argument " + debugNode(left) + " resolved to null" +
                            "\n(from " + debugNode(expr) + ")");
                    }
                    final String rightString = resolveString(ctx, right);
                    if (rightString == null) {
                        throw new IllegalArgumentException("Invalid right argument " + debugNode(right) + " resolved to null" +
                            "\n(from " + debugNode(expr) + ")");
                    }
                    boolean equal = leftString.equals(rightString);
                    if (expr.getOperator() == Operator.equals) {
                        return boolLiteral(equal);
                    } else if (expr.getOperator() == Operator.notEquals) {
                        return boolLiteral(!equal);
                    } else {
                        throw new IllegalStateException("Can't get here; bad node: " + debugNode(expr));
                    }
            }
        } else if (valueExpr instanceof ConditionalExpr) {
            ConditionalExpr conditional = (ConditionalExpr) valueExpr;
            final Expression condition = resolveVar(ctx, conditional.getCondition());
            if (((BooleanLiteralExpr)condition).getValue()) {
                return resolveVar(ctx, conditional.getThenExpr());
            } else {
                return resolveVar(ctx, conditional.getElseExpr());

            }
        } else if (valueExpr instanceof MethodCallExpr) {
            final Maybe<AstMethodInvoker<Ctx>> invoker = methods().findMethod(ctx, (MethodCallExpr) valueExpr);
            if (invoker.isPresent()) {
                final AstMethodResult result = invoker.get().io(this, ctx, (MethodCallExpr) valueExpr);
                CountedIterator<Expression> all = CountedIterator.count(result.getExprs());
                if (all.size() == 1) {
                    return all.next();
                }
                final JsonContainerExpr json = new JsonContainerExpr(() -> all);
                json.addExtra("isWrappedVararg", true);
                return json;
            }
        } else if (valueExpr instanceof SysExpr) {
            SysExpr sys = (SysExpr) valueExpr;
            return sys.readAsOneNode((n, c)-> {
                final Expression resolved = resolveNode(ctx, n);

                ComposableXapiVisitor<Ctx> nameVisitor = new ComposableXapiVisitor<>();
                nameVisitor.withNameExpr((name, con)->{
                    String newName = resolveTemplate(con, templateLiteral(name.getName()));
                    name.setName(newName);
                    return false;
                });
                nameVisitor.withMethodCallExpr((name, con)->{
                    String newValue = resolveTemplate(con, templateLiteral(name.getName()));
                    name.setName(newValue);
                    return true;
                });
                nameVisitor.withStringLiteralExpr((name, con)->{
                    String newValue = resolveTemplate(con, templateLiteral(name.getValue()));
                    name.setValue(newValue);
                    return false;
                });
                nameVisitor.withTemplateLiteralExpr((name, con)->{
                    String newValue = resolveTemplate(con, name);
                    name.setValue(newValue);
                    return false;
                });
                resolved.accept(nameVisitor, c);
                return resolved;
            }, ctx);
        } else if (valueExpr instanceof NameExpr) {
            String key = ((NameExpr)valueExpr).getName();
            if (ctx.hasNode(key)) {
                return (Expression) ctx.getNode(key);
            }
        }
        return valueExpr;
    }

    default boolean check(Operator operator, Number left, Number right) {
        switch (operator) {
            case less:
                return left.doubleValue() < right.doubleValue();
            case lessEquals:
                return left.doubleValue() <= right.doubleValue();
            case greater:
                return left.doubleValue() > right.doubleValue();
            case greaterEquals:
                return left.doubleValue() >= right.doubleValue();
            case equals:
                return left.doubleValue() == right.doubleValue();
            case notEquals:
                return left.doubleValue() != right.doubleValue();
            default:
                throw new IllegalArgumentException("Cannot coerce number to boolean for operator " + operator);
        }
    }

    default char apply(Operator operator, Character left, Character right) {
        // nasty hack, but it'll do the trick...
        return (char) + apply(operator, (int)left.charValue(), (int)right.charValue());
    }

    default int apply(Operator operator, Integer left, Integer right) {
        switch (operator) {
            case binAnd:
                return left & right;
            case binOr:
                return left | right;
            case divide:
                return left / right;
            case lShift:
                return left << right;
            case minus:
                return left - right;
            case plus:
                return left + right;
            case remainder:
                return left % right;
            case rSignedShift:
                return left >> right;
            case rUnsignedShift:
                return left >>> right;
            case times:
                return left * right;
            case xor:
                return left ^ right;
            default:
                throw new UnsupportedOperationException("Cannot apply " + operator + " to " + left + " and " + right);
        }
    }

    default long apply(Operator operator, Long left, Long right) {
        switch (operator) {
            case binAnd:
                return left & right;
            case binOr:
                return left | right;
            case divide:
                return left / right;
            case lShift:
                return left << right;
            case plus:
                return left + right;
            case minus:
                return left - right;
            case remainder:
                return left % right;
            case rSignedShift:
                return left >> right;
            case rUnsignedShift:
                return left >>> right;
            case times:
                return left * right;
            case xor:
                return left ^ right;
            default:
                throw new UnsupportedOperationException("Cannot apply " + operator + " to " + left + " and " + right);
        }
    }

    default double apply(Operator operator, Double left, Double right) {
        switch (operator) {
            case divide:
                return left / right;
            case minus:
                return left - right;
            case plus:
                return left + right;
            case remainder:
                return left % right;
            case times:
                return left * right;
            default:
                throw new UnsupportedOperationException("Cannot apply " + operator + " to " + left + " and " + right);
        }
    }

    default boolean areIntTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, IntegerLiteralExpr.class, exprs);
    }

    default boolean arePrimitiveLiterals(Ctx ctx, Expression... exprs) {
        return areIntTypes(ctx, exprs) || areDoubleTypes(ctx, exprs) || areLongTypes(ctx, exprs) || areCharTypes(ctx, exprs);
    }

    default boolean areDoubleTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, DoubleLiteralExpr.class, exprs);
    }

    default boolean areCharTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, CharLiteralExpr.class, exprs);
    }

    default boolean areBooleanTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, BooleanLiteralExpr.class, exprs);
    }

    default boolean areStringTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, StringLiteralExpr.class, exprs);
    }

    default boolean areLongTypes(Ctx ctx, Expression... exprs) {
        return areType(ctx, LongLiteralExpr.class, exprs);
    }

    default Node lookupNode(Ctx ctx, Node expr) {
        if (expr instanceof NameExpr) {
            String name = ((NameExpr)expr).getName();
            if (ctx.hasNode(name)) {
                Node found = ctx.getNode(name);
                return found == expr ? found : lookupNode(ctx, found);
            }
        } else if (expr instanceof TemplateLiteralExpr) {
            String key = resolveString(ctx, (TemplateLiteralExpr)expr);
            if (ctx.hasNode(key)) {
                Node found = ctx.getNode(key);
                return found == expr ? found : lookupNode(ctx, found);
            }
        }
        return expr;
    }

    default boolean areType(Ctx ctx, Class<? extends Node> nodeCls, Expression... exprs) {
        boolean isMatch = false;
        for (Expression expr : exprs) {
            if (isType(nodeCls, expr)) {
                isMatch = true;
            } else if (expr instanceof NameExpr) {
                String name = ((NameExpr)expr).getName();
                if (isType(nodeCls, ctx.getNode(name))) {
                    isMatch = true;
                } else {
                    return false;
                }
            } else if (expr instanceof TemplateLiteralExpr) {
                String key = resolveString(ctx, expr);
                if (isType(nodeCls, ctx.getNode(key))) {
                    isMatch = true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return isMatch;
    }

    default boolean isType(Class<? extends Node> nodeCls, Node node) {
        return node != null &&
            (
                nodeCls == StringLiteralExpr.class ? node.getClass() == nodeCls :
                nodeCls.isInstance(node)
            );
    }

    default Expression boxResult(Ctx ctx, MethodCallExpr expr, Object result) {
        if (result instanceof Expression) {
            return (Expression) result;
        }
        if (result instanceof Number) {
            if (result.getClass() == Integer.class) {
                return IntegerLiteralExpr.intLiteral((Integer)result);
            }
            return DoubleLiteralExpr.doubleLiteral(((Number)result).doubleValue());
        }
        if (result instanceof String) {
            return StringLiteralExpr.stringLiteral((String) result);
        }
        if (result instanceof Boolean) {
            return BooleanLiteralExpr.boolLiteral((Boolean)result);
        }
        if (result.getClass().isArray()) {
            List<Expression> items = new ArrayList<>();
            for (int i = 0, m = X_Fu.getLength(result); i < m; i++) {
                Object value = X_Fu.getValue(result, i);
                Expression boxed = boxResult(ctx, expr, value);
                items.add(boxed);
            }
            return JsonContainerExpr.jsonArray(items);
        }
        throw new UnsupportedOperationException("Unable to box result type " + result);
    }

    default String getPackage(Ctx ctx, Node n, Out1<String> dfltPackage) {
        final Expression expr;
        if (n instanceof UiContainerExpr) {
            UiContainerExpr container = (UiContainerExpr) n;
            expr = container.getAttribute("package")
                .ifAbsentReturn(UiAttrExpr.of("package", dfltPackage.out1()
            ));
            return getPackage(ctx, expr, dfltPackage);
        } else if (n instanceof UiAttrExpr) {
            UiAttrExpr attr = (UiAttrExpr) n;
            if (!"package".equals(attr.getNameString())) {
                throw new IllegalArgumentException("Cannot get a package from an attribute not named `package`; " + debugNode(n));
            }
            return getPackage(ctx, attr.getExpression(), dfltPackage);
        } else if (n instanceof CompilationUnit) {
            CompilationUnit unit = (CompilationUnit) n;
            expr = unit.getPackage().getName();
            if (ctx == null) {
                return ASTHelper.extractStringValue(expr);
            }
            return resolveString(ctx, expr);
        } else if (n instanceof Expression) {
            if (ctx == null) {
                return ASTHelper.extractStringValue((Expression)n);
            }
            return resolveString(ctx, (Expression) n);
        } else {
            throw new IllegalArgumentException("Cannot extract package from " + debugNode(n));
        }
    }

    default String lookupType(ImportSection buf, String s) {
        String tryOne = WellKnownTypes.qualifyType(s);
        if (s.equals(tryOne)) {
            return buf.qualify(s);
        }
        return tryOne;
    }

    StringTo<String> DEFAULT_MAP_TYPES = X_Collect.newStringMap(
        "Map", "java.util.Map",
        "java.util.Map", "java.util.Map",

        "HashMap", "java.util.HashMap",
        "java.util.HashMap", "java.util.HashMap",

        "LinkedHashMap", "java.util.LinkedHashMap",
        "java.util.LinkedHashMap", "java.util.LinkedHashMap",

        "TreeMap", "java.util.TreeMap",
        "java.util.TreeMap", "java.util.TreeMap",

        "ConcurrentMap", "java.util.concurrent.ConcurrentMap",
        "java.util.concurrent.ConcurrentMap", "java.util.concurrent.ConcurrentMap",

        "ConcurrentHashMap", "java.util.concurrent.ConcurrentHashMap",
        "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentHashMap",

        "ConcurrentSkipListMap", "java.util.concurrent.ConcurrentSkipListMap",
        "java.util.concurrent.ConcurrentSkipListMap", "java.util.concurrent.ConcurrentSkipListMap",

        "MapLike", "xapi.fu.data.MapLike",
        "xapi.fu.data.MapLike", "xapi.fu.data.MapLike",

        "StringTo", "xapi.collect.api.StringTo",
        "xapi.collect.api.StringTo", "xapi.collect.api.StringTo",
        "StringTo.Many", "xapi.collect.api.StringTo.Many",
        "xapi.collect.api.StringTo.Many", "xapi.collect.api.StringTo.Many",
        "StringTo$Many", "xapi.collect.api.StringTo.Many",
        "xapi.collect.api.StringTo$Many", "xapi.collect.api.StringTo.Many",

        "ObjectTo", "xapi.collect.api.ObjectTo",
        "xapi.collect.api.ObjectTo", "xapi.collect.api.ObjectTo",
        "ObjectTo.Many", "xapi.collect.api.ObjectTo.Many",
        "xapi.collect.api.ObjectTo.Many", "xapi.collect.api.ObjectTo.Many",
        "ObjectTo$Many", "xapi.collect.api.ObjectTo.Many",
        "xapi.collect.api.ObjectTo$Many", "xapi.collect.api.ObjectTo.Many",

        "ClassTo", "xapi.collect.api.ClassTo",
        "xapi.collect.api.ClassTo", "xapi.collect.api.ClassTo",
        "ClassTo.Many", "xapi.collect.api.ClassTo.Many",
        "xapi.collect.api.ClassTo.Many", "xapi.collect.api.ClassTo.Many",
        "ClassTo$Many", "xapi.collect.api.ClassTo.Many",
        "xapi.collect.api.ClassTo$Many", "xapi.collect.api.ClassTo.Many",

        "EnumTo", "xapi.collect.api.EnumTo",
        "xapi.collect.api.EnumTo", "xapi.collect.api.EnumTo",
        "EnumTo.Many", "xapi.collect.api.EnumTo.Many",
        "xapi.collect.api.EnumTo.Many", "xapi.collect.api.EnumTo.Many",
        "EnumTo$Many", "xapi.collect.api.EnumTo.Many",
        "xapi.collect.api.EnumTo$Many", "xapi.collect.api.EnumTo.Many",

        "AssignabilityMap", "xapi.collect.api.AssignabilityMap",
        "xapi.collect.api.AssignabilityMap", "xapi.collect.api.AssignabilityMap"

    );
    StringTo<String> DEFAULT_LIST_TYPES = X_Collect.newStringMap(
        "List", "java.util.List",
        "java.util.List", "java.util.List",
        "Set", "java.util.Set",
        "java.util.Set", "java.util.Set",
        "Queue", "java.util.Queue",
        "java.util.Queue", "java.util.Queue",
        "Dequeue", "java.util.Dequeue",
        "java.util.Dequeue", "java.util.Dequeue",

        "ArrayList", "java.util.ArrayList",
        "java.util.ArrayList", "java.util.ArrayList",
        "LinkedList", "java.util.LinkedList",
        "java.util.LinkedList", "java.util.LinkedList",

        "HashSet", "java.util.HashSet",
        "java.util.HashSet", "java.util.HashSet",
        "LinkedHashSet", "java.util.LinkedHashSet",
        "java.util.LinkedHashSet", "java.util.LinkedHashSet",
        "TreeSet", "java.util.TreeSet",
        "java.util.TreeSet", "java.util.TreeSet",

        "ConcurrentLinkedDeque", "java.util.concurrent.ConcurrentLinkedDequeue",
        "java.util.concurrent.ConcurrentLinkedDeque", "java.util.concurrent.ConcurrentLinkedDequeue",

        "ListLike", "xapi.fu.data.ListLike",
        "xapi.fu.data.ListLike", "xapi.fu.data.ListLike",
        "SetLike", "xapi.fu.data.SetLike",
        "xapi.fu.data.SetLike", "xapi.fu.data.SetLike",

        "Fifo", "xapi.collect.api.Fifo",
        "xapi.collect.api.Fifo", "xapi.collect.api.Fifo",

        "IntTo", "xapi.collect.api.IntTo",
        "xapi.collect.api.IntTo", "xapi.collect.api.IntTo",
        "IntTo.Many", "xapi.collect.api.IntTo.Many",
        "xapi.collect.api.IntTo.Many", "xapi.collect.api.IntTo.Many",
        "IntTo$Many", "xapi.collect.api.IntTo.Many",
        "xapi.collect.api.IntTo$Many", "xapi.collect.api.IntTo.Many",

        "SimpleFifo", "xapi.collect.impl.SimpleFifo",
        "xapi.collect.impl.SimpleFifo", "xapi.collect.impl.SimpleFifo",
        "SimpleLinkedList", "xapi.collect.impl.SimpleLinkedList",
        "xapi.collect.impl.SimpleLinkedList", "xapi.collect.impl.SimpleLinkedList"
    );//TODO: .immutable()

    StringTo<String> DEFAULT_ITERATING_TYPES = X_Collect.newStringMap(
        "Iterator", "java.util.Iterator",
        "java.util.Iterator", "java.util.Iterator",

        "MappedIterable", "xapi.fu.itr.MappedIterable",
        "xapi.fu.itr.MappedIterable", "xapi.fu.itr.MappedIterable",
        "Chain", "xapi.fu.itr.Chain",
        "xapi.fu.itr.Chain", "xapi.fu.itr.Chain",
        "ChainBuilder", "xapi.fu.itr.ChainBuilder",
        "xapi.fu.itr.ChainBuilder", "xapi.fu.itr.ChainBuilder",
        "ArrayIterable", "xapi.fu.itr.ArrayIterable",
        "xapi.fu.itr.ArrayIterable", "xapi.fu.itr.ArrayIterable",
        "CachingIterator", "xapi.fu.itr.CachingIterator",
        "xapi.fu.itr.CachingIterator", "xapi.fu.itr.CachingIterator"
    );

    default MappedIterable<String> allMapTypes() {
        return DEFAULT_MAP_TYPES.keys();
    }

    default MappedIterable<String> allListTypes() {
        return DEFAULT_LIST_TYPES.keys();
    }


    default Expression qualified(Ctx ctx, Expression scope, Expression name) {
        return qualified(ctx, scope, name, In2.NULL);
    }

    default Expression qualified(
        Ctx ctx,
        Expression scope,
        Expression name,
        In2<NameExpr, Expression> spyQualified
    ) {
        assert name != null;
        if (scope == null) {
            return name;
        }
        if (name instanceof ScopedExpression) {
            ScopedExpression n = (ScopedExpression) name;
            assert n.getScope() == null || n.getScope() == scope :
                "Do not redefine a scoped expression;" +
                    "\nwas: " + n.getScope() +
                    "\nto:  " + scope +
                    "\nfor: " + name;
            n.setScope(scope);
            if (scope instanceof NameExpr) {
                // todo: import / spy on the qualified name of a method / field / type reference
                if (spyQualified != null) {
                    spyQualified.in((NameExpr)scope, name);
                }
            }
            return name;
        }
        if (scope instanceof NameExpr) {
            spyQualified.in((NameExpr) scope, name);
        }
        if (! (name instanceof NameExpr)) {
            name = coerceToName(ctx, name);
        }
        if (name instanceof NameExpr) {
            NameExpr n = (NameExpr) name;
            if ("class".equals(n.getSimpleName())) {
                // when we have a class literal,
                // lets collapse it into a type expression.
                if (scope instanceof TypeExpr) {
                    // Ok, our scope is already in the form of Type.class
                    return scope;
                } else {
                    // Create a type out of the name of this scope (dropping the .class)
                    return methods().$type(this, ctx, scope);
                }
            }
            if (scope instanceof NameExpr ) {
                // com.fu.Type.fieldName or com.fu.Type
                final NameExpr scopeName = (NameExpr) scope;
                if (Character.isUpperCase(scopeName.getSimpleName().charAt(0))) {
                    // if you follow java naming conventions; packages lowercase,
                    // all TypeNames title case, then we can infer field / nested types.
                    if (Character.isUpperCase(n.getSimpleName().charAt(0))) {
                        // Nested.Type
                        return asNestedType(ctx, scopeName, n);
                    } else {
                        // Field.reference
                        return asFieldReference(ctx, scopeName, n);
                    }
                } else if (Character.isUpperCase(n.getSimpleName().charAt(0))) {
                    // pkgName.TypeName
                    final ClassOrInterfaceType type = new ClassOrInterfaceType(X_Source.qualifiedName(
                        scopeName.getQualifiedName(),
                        n.getQualifiedName()
                    ));
                    return new TypeExpr(type);
                } else {
                    // com.pkgName // we don't actually have a package type expression;
                    // just return a standard QualifiedName
                    return new QualifiedNameExpr(scopeName, n.getQualifiedName());
                }
            } else if (scope instanceof TypeExpr){
                // When the scope is a type, is the name []?
                if ("[]".equals(n.getName())) {
                    final Type scopeType = ((TypeExpr) scope).getType();
                    if (scopeType instanceof ReferenceType) {
                        final ReferenceType refType = (ReferenceType)scopeType;
                        refType.setArrayCount(refType.getArrayCount()+1);
                        return scope;
                    } else {
                        ReferenceType asArray = new ReferenceType(scopeType, 1);
                        return new TypeExpr(asArray);
                    }
                }
            }
        }
        throw new UnsupportedOperationException("Cannot handle name " + debugNode(name) + " with scope (" + debugNode(scope) + ")");
    }

    default Expression coerceToName(Ctx ctx, Expression name) {
        if (name instanceof NameExpr) {
            return name;
        } else if (name instanceof TemplateLiteralExpr) {
            return new NameExpr(resolveTemplate(ctx, (TemplateLiteralExpr) name));
        }
        return name;
    }

    default Expression asFieldReference(Ctx ctx, NameExpr scopeName, NameExpr n) {
        final TypeExpr rootType = methods().$type(this, ctx, scopeName);
        FieldAccessExpr field = new FieldAccessExpr(rootType, n.getQualifiedName());
        return field;
    }

    default Expression asNestedType(Ctx ctx, NameExpr scopeName, NameExpr n) {
        final TypeExpr rootType = methods().$type(this, ctx, scopeName);
        if (n == null) {
            return rootType;
        }
        if ("[]".equals(n.getSimpleName())) {
            final ReferenceType refType = new ReferenceType(rootType.getType(), 1);
            return new TypeExpr(refType);
        }
        if ("class".equals(n.getSimpleName())) {
            return new ClassExpr(rootType.getType());
        }
        final ClassOrInterfaceType qualified = new ClassOrInterfaceType((ClassOrInterfaceType) rootType.getType(), n.getQualifiedName());
        return new TypeExpr(qualified);
    }

    default String[] getStringMembers(Ctx ctx, AnnotationExpr expr) {
        return expr.getMembers().map(pair-> {
            final Expression val = pair.getValue();
            return resolveToLiterals(ctx, val);
        }).flatten(IntTo::forEach).toArray(String[]::new);
    }

    default void maybeAddImports(
        Ctx ctx,
        HasSourceBuilder<?> api,
        UiAttrExpr attr
    ) {
        // This is a really handy method... we should move it somewhere upstream for others to enjoy.
        // like putting it in GeneratedJavaFile, or UiComponentGenerator (or both)
        attr.getAnnotationsMatching(anno->X_String.containsAny(anno.getNameString().toLowerCase(), "import", "importstatic", "staticimport"))
            .forAll(anno->anno.getMembers().forEach(pair->{
                boolean isStatic = anno.getNameString().toLowerCase().contains("static");
                In1<String> addImport = isStatic ? api.getSource()::addImportStatic : api.getSource()::addImport;
                final Expression resolvedImport = resolveVar(ctx, pair.getValue());
                String toImport;
                if (resolvedImport instanceof StringLiteralExpr) {
                    toImport = ((StringLiteralExpr)resolvedImport).getValue();
                    addImport.in(toImport);
                } else if (resolvedImport instanceof TemplateLiteralExpr) {
                    toImport = resolveTemplate(ctx, (TemplateLiteralExpr) resolvedImport);
                    addImport.in(toImport);
                } else if (resolvedImport instanceof FieldAccessExpr) {
                    final FieldAccessExpr asField = (FieldAccessExpr) resolvedImport;
                    toImport = resolveString(ctx, asField.getScope()) + "." + asField.getField();
                    addImport.in(toImport);
                } else if (resolvedImport instanceof ArrayInitializerExpr) {
                    ArrayInitializerExpr many = (ArrayInitializerExpr) resolvedImport;
                    for (Expression expr : many.getValues()) {
                        toImport = resolveString(ctx, expr);
                        addImport.in(toImport);
                    }
                } else {
                    throw new IllegalArgumentException("Unhandled @Import value " + debugNode(resolvedImport));
                }
            }));

    }
}
