package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.Rethrowable;
import xapi.source.write.Template;
import xapi.util.X_String;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/22/16.
 */
public interface ApiGeneratorTools <Ctx extends ApiGeneratorContext<Ctx>> extends Rethrowable {

    default void addTypeParams(Ctx ctx, SourceBuilder<Ctx> builder, Expression typeParams) {
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

    default IntTo<String> resolveToLiterals(Ctx ctx, Expression typeParams) {
        IntTo<String> types = X_Collect.newList(String.class);
        if (typeParams instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) typeParams;
            if (call.getName().startsWith("$")) {
                // We have a utility method to deal with;
                final List<Expression> args = call.getArgs();
                switch (call.getName()) {
                    case "$range":
                        // expects a arguments in the forms of literals or lambda expressions
                        List<String> items = resolveRange(ctx, call);
                        types.addAll(items);
                        break;
                    case "$type":
                        // first argument is the type name,
                        String type = resolveString(ctx, args.get(0));
                        if (args.size() > 1) {
                            StringBuilder b = new StringBuilder(type).append("<");
                            String prefix = "";
                            for (int i = 1; i < args.size(); i++) {
                                final Expression nextArg = args.get(i);
                                final IntTo<String> generics = resolveToLiterals(ctx, nextArg);
                                for (String s : generics.forEach()) {
                                    b.append(prefix);
                                    b.append(s);
                                    prefix = ", ";
                                }
                            }
                            b.append(">");
                            type = b.toString();
                        }
                        types.add(type);
                        break;
                    case "$replace":
                        // get the types from the scope of the method call,
                        // then perform replacement on them.
                        if (call.getScope() == null) {
                            throw new IllegalArgumentException("$replace can only be used after another expression; " +
                                "such as: $range(1, 2, $n->`I$n`).$replace(`I1`, `To`)");
                        }
                        if (args.size() != 2) {
                            throw new IllegalArgumentException("$replace() calls must have exactly two arguments");
                        }
                        IntTo<String> scoped = resolveToLiterals(ctx, call.getScope());
                        String pattern = resolveString(ctx, args.get(0));
                        String replaceWith = resolveString(ctx, args.get(1));
                        Pattern regex = Pattern.compile(pattern);
                        for (int i = 0; i < scoped.size(); i++) {
                            final Matcher matcher = regex.matcher(scoped.at(i));
                            if (matcher.matches()) {
                                final String result = matcher.replaceAll(replaceWith);
                                scoped.set(i, result);
                            }
                        }
                        return scoped;
                    case "$generic":
                        // Method should be of the form:
                        // Type.class.$generic("G", "E", "NERIC" ...)
                        scoped = resolveToLiterals(ctx, call.getScope());
                        AtomicInteger i = new AtomicInteger();
                        JsonContainerExpr wrapper = new JsonContainerExpr(true, args
                            .stream()
                            .map(e->new JsonPairExpr(Integer.toString(i.getAndIncrement()), e))
                            .collect(Collectors.toList())
                        );
                        final IntTo<String> generics = resolveToLiterals(ctx, wrapper);
                        String generic = "<" + generics.join(", ") + ">";
                        for (String raw : scoped.forEach()) {
                            types.add(raw + generic);
                        }
                        break;
                }
            }
        } else if (typeParams instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) typeParams;
            if (json.isArray()) {
                json.getPairs().forEach(pair->{
                    final IntTo<String> result = resolveToLiterals(ctx, pair.getValueExpr());
                    types.addAll(result);
                });
            } else {
                json.getPairs().forEach(pair-> {
                    final IntTo<String> paramType = resolveToLiterals(ctx, pair.getValueExpr());
                    if (paramType.size() != 1) {
                        throw new IllegalArgumentException("Parameter types using json notation must have " +
                            "values which return exactly one type;" +
                            "\nyou sent " + debugNode(pair.getValueExpr()) + "" +
                            "\nwhich returned " + paramType +
                            "\nfrom " + debugNode(json));
                    }
                    types.addAll(paramType.get(0) + " " + pair.getKeyString());
                });
            }
        } else if (typeParams instanceof TemplateLiteralExpr){
            String result = resolveTemplate(ctx, ((TemplateLiteralExpr)typeParams));
            types.add(result);
        } else if (typeParams instanceof StringLiteralExpr){
            types.add(ASTHelper.extractStringValue(typeParams));
        } else if (typeParams instanceof QualifiedNameExpr){
            QualifiedNameExpr qualified = (QualifiedNameExpr) typeParams;
            if ("class".equals(qualified.getSimpleName())) {
                typeParams = qualified.getQualifier();
            } else {
                typeParams = qualified;
            }
            types.add(((NameExpr)typeParams).getName());
        } else if (typeParams instanceof NameExpr) {
            String name = ((NameExpr)typeParams).getName();
            if (name.startsWith("$")) {
                types.add(ctx.getString(name));
            } else {
                types.add(name);
            }
        } else if (typeParams instanceof ClassExpr) {
            Type type = ((ClassExpr)typeParams).getType();
            int arrayCnt = 0;
            if (type instanceof ReferenceType) {
                arrayCnt = ((ReferenceType)type).getArrayCount();
                type = ((ReferenceType)type).getType();
            }
            String arrays = X_String.repeat("[]", arrayCnt);
            if (type instanceof VoidType) {
                types.add("void");
            } else if (type instanceof ClassOrInterfaceType) {
                types.add(((ClassOrInterfaceType)type).getName() + arrays);
            } else {
                throw new IllegalArgumentException("Unhandled ClassExpr type " + type + " from " + typeParams + " at " + typeParams.getCoordinates());
            }
        } else {
            throw new IllegalArgumentException("Unhandled type argument: " + typeParams + " of type " + typeParams.getClass() + " at " + typeParams.getCoordinates());
        }
        return types;
    }

    default String debugNode(Node node) {
        return node == null ? "null" : node + " of type " + node.getClass() + " at " + node.getCoordinates();
    }

    default String resolveString(Ctx ctx, Expression value) {
        if (value instanceof TemplateLiteralExpr) {
            return resolveTemplate(ctx, (TemplateLiteralExpr) value);
        } else if (value instanceof StringLiteralExpr) {
            return ((StringLiteralExpr)value).getValue();
        } else if (value instanceof NameExpr) {
            String name = ((NameExpr)value).getName();
            if (name.startsWith("$")) {
                return ctx.getString(name);
            } else {
                return name;
            }
        } else {
            throw new IllegalArgumentException("Unable to extract string from " + value.getClass() + " : " + value + " @ " + value.getCoordinates());
        }
    }
    default String resolveTemplate(Ctx ctx, TemplateLiteralExpr value) {
        return ctx.resolveValues(value.getValueWithoutTicks());
    }

    default List<String> resolveRange(Ctx ctx, MethodCallExpr call) {
        final List<String> resolved = new ArrayList<>();
        final List<Expression> args = call.getArgs();

        if (args.size() != 3) {
            throw new IllegalArgumentException("$range lambdas must " +
                "have exactly three arguments; you sent " + args + " at " + call.getCoordinates());
        }
        int start = extractInt(ctx, args.get(0));
        int end = extractInt(ctx, args.get(1));
        final Expression result = args.get(2);
        if (result instanceof LambdaExpr) {
            LambdaExpr lambda = (LambdaExpr) result;
            if (lambda.getParameters().size() != 1) {
                throw new IllegalArgumentException("$range lambdas must have " +
                    "exactly one argument; you sent " + lambda + " at " + lambda.getCoordinates());
            }
            String varName = lambda.getParameters().get(0).getId().getName();
            for (; start <= end; start ++ ) {
                final Do undo = ctx.addToContext(varName, IntegerLiteralExpr.intLiteral(start));
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
        return resolved;
    }

    default int extractInt(Ctx ctx, Expression parameter) {
        if (parameter instanceof StringLiteralExpr) {
            return Integer.parseInt(ASTHelper.extractStringValue(parameter));
        } else if (parameter instanceof NameExpr) {
            String name = ((NameExpr)parameter).getName();
            if (name.startsWith("$")) {
                name = name.substring(1);
            }
            // Now that we have the variable name, lets pull it from context
            int size = Integer.parseInt(ctx.getString(name));
            return size;
        }
        throw new IllegalArgumentException("Cannot extract int from " + parameter + " (at " + parameter.getCoordinates() + ")");
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
                    throw new IllegalArgumentException("Unhandled binary operator " + expr.getOperator() + " in " + debugNode(valueExpr));
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

    default boolean isConditionTrue(Ctx ctx, Expression condition) {
        if (condition instanceof BinaryExpr) {
            BinaryExpr expr = (BinaryExpr) condition;
            String leftValue = resolveString(ctx, expr.getLeft());
            String rightValue = resolveString(ctx, expr.getRight());
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
        } else if (condition instanceof EnclosedExpr) {
            return isConditionTrue(ctx, ((EnclosedExpr)condition).getInner());
        }
        return false;
    }
}
