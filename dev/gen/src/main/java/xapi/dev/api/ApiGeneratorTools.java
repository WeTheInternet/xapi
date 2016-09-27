package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.Filter.Filter1;
import xapi.fu.Rethrowable;
import xapi.source.write.Template;
import xapi.util.X_String;

import static com.github.javaparser.ast.expr.BooleanLiteralExpr.boolLiteral;
import static com.github.javaparser.ast.expr.CharLiteralExpr.charLiteral;
import static com.github.javaparser.ast.expr.DoubleLiteralExpr.doubleLiteral;
import static com.github.javaparser.ast.expr.IntegerLiteralExpr.intLiteral;
import static com.github.javaparser.ast.expr.LongLiteralExpr.longLiteral;
import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        JsonContainerExpr wrapper = JsonContainerExpr.jsonArray(args);
                        final IntTo<String> generics = resolveToLiterals(ctx, wrapper);
                        String generic = "<" + generics.join(", ") + ">";
                        for (String raw : scoped.forEach()) {
                            types.add(raw + generic);
                        }
                        break;
                    case "$first":
                        types.add(Boolean.toString(ctx.isFirstOfRange()));
                        break;
                    case "$remove":
                        scoped = resolveToLiterals(ctx, call.getScope());
                        if (args.size() != 1) {
                            throw new IllegalArgumentException("$remove() expects exactly one argument;" +
                                "\nyou sent " + debugNode(call));
                        }
                        final Expression asVar = resolveVar(ctx, args.get(0));
                        pattern = resolveString(ctx, asVar);
                        regex = Pattern.compile(pattern);
                        scoped.removeIf(s->regex.matcher(s).matches(), true);
                        return scoped;
                    default:
                        throw new NotYetImplemented("System method " + call.getName()
                         +" is either misspelled or not supported;\n" + debugNode(call));
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
                    types.addAll(paramType.get(0) + " " + name);
                });
            }
        } else if (typeParams instanceof TemplateLiteralExpr){
            String result = resolveTemplate(ctx, ((TemplateLiteralExpr)typeParams));
            types.add(result);
        } else if (typeParams instanceof StringLiteralExpr){
            types.add(ASTHelper.extractStringValue(typeParams));
        } else if (typeParams instanceof BooleanLiteralExpr){
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
        } else if (typeParams instanceof BinaryExpr) {
            final Expression result = resolveVar(ctx, typeParams);
            return resolveToLiterals(ctx, result);
        } else if (typeParams instanceof ConditionalExpr) {
            ConditionalExpr conditional = (ConditionalExpr) typeParams;
            if (isConditionTrue(ctx, conditional.getCondition())) {
                return resolveToLiterals(ctx, conditional.getThenExpr());
            } else {
                return resolveToLiterals(ctx, conditional.getElseExpr());
            }
        } else {
            throw new IllegalArgumentException("Unhandled type argument: " + debugNode(typeParams));
        }
        return types;
    }

    default String debugNode(Node node) {
        return node == null ? "null" : node + " of type " + node.getClass() + " at " + node.getCoordinates();
    }

    default String resolveString(Ctx ctx, Expression value) {
        final IntTo<String> literals = resolveToLiterals(ctx, value);
        if (literals.size() != 1) {
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
        return ctx.resolveValues(value.getValueWithoutTicks());
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
            throw new IllegalArgumentException("Cannot ");
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
                      String rightString = resolveString(ctx, expr.getLeft());
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

}
