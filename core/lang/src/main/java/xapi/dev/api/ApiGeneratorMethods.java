package xapi.dev.api;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeArguments;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.AstMethodInvoker.AstMethodResult;
import xapi.fu.*;
import xapi.fu.has.HasSize;
import xapi.fu.iterate.ChainBuilder;
import xapi.util.X_Debug;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;
import static xapi.fu.Immutable.immutable1;

/**
 * This interface is used to define the $globalMethods()
 * used in xapi generator templates; if you implement / extend this interface,
 * then you can replace the default instance used in a {@link ApiGeneratorTools}
 *
 * The default behavior is for all public methods in the `this` instance
 * to be scanned and returned, with instances of {@link ApiGeneratorTools}
 * or {@link ApiGeneratorContext} being optional, provided they are the
 * first arguments to a method, they will be mapped automatically.
 *
 * Every method must take at least one Expression,
 * and return an Iterable&lt;Expression> as its replacement;
 * or null to skip replacement.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/30/16.
 */
@SuppressWarnings("unused") // These methods are used via reflection from xapi script
public interface ApiGeneratorMethods<Ctx extends ApiGeneratorContext<Ctx>> extends Rethrowable {

    ApiGeneratorTools<Ctx> tools();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface SystemProvided {}

    default boolean $first(Ctx ctx) {
        return ctx.isFirstOfRange();
    }

    default TemplateLiteralExpr $print(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression arg) {
        final Expression resolved = tools.resolveVar(ctx, arg);
        return templateLiteral(resolved.toSource());
    }

    default Expression $if(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression condition, Expression thenClause) {
        final Expression result = tools.resolveVar(ctx, condition);
        if (tools.isConditionTrue(ctx, result)) {
            final Expression value = tools.resolveVar(ctx, thenClause);
            return value;
        }
        return null;
    }

    default Expression $else(ApiGeneratorTools<Ctx> tools, Ctx ctx, @SystemProvided MethodCallExpr method, Expression thenClause) {
        final Expression scope = method.getScope();
        if (scope == null) {
            throw new NullPointerException("$else() invocation must be chained to a $if() invocation. $if(...).$else(...)");
        }
        if (!(scope instanceof MethodCallExpr)) {
            throw new IllegalArgumentException("$else() invocation must be chained to a $if() invocation. $if(...).$else(...).  You sent " + tools.debugNode(method));
        }
        final Expression ifClause = ((MethodCallExpr) scope).getArgs().get(0);
        final Expression result = tools.resolveVar(ctx, ifClause);
        if (tools.isConditionTrue(ctx, result)) {
            final Expression value = tools.resolveVar(ctx, ((MethodCallExpr) scope).getArgs().get(1));
            return value;
        } else {
            final Expression value = tools.resolveVar(ctx, thenClause);
            return value;
        }
    }

    default TypeExpr $generic(ApiGeneratorTools<Ctx> tools, Ctx ctx, @SystemProvided MethodCallExpr call, Expression ... args) {
        final Expression scope = call.getScope();
        assert scope != null : ".$generic() must be used with scope; like Type.class.$generic(`T$n`)" +
            "\nYou sent " + tools.debugNode(call);
        final TypeExpr scopeType = $type(tools, ctx, scope);
        List<Type> generics = new ArrayList<>(args.length);
        for (Expression arg : args) {
            final TypeExpr argType = $type(tools, ctx, arg);
            generics.add(argType.getType());
        }
        final Expression resolvedScope = tools.resolveVar(ctx, scope);
        final String scopeSource = tools.resolveString(ctx, resolvedScope);
        ClassOrInterfaceType t = new ClassOrInterfaceType(scopeSource);
        t.setTypeArguments(TypeArguments.withArguments(generics));
        return new TypeExpr(t);
    }

    default TypeExpr $type(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression name, Expression ... generics) {
        String named = tools.resolveString(ctx, name, true);
        final List<ReferenceType> refs = new ArrayList<>(generics.length);
        for (Expression generic : generics) {
            final Expression resolved = tools.resolveVar(ctx, generic);
            final TypeExpr superType = $type(tools, ctx, resolved);
            final String typeString = tools.resolveString(ctx, resolved, true);
            if (!typeString.isEmpty()) {
                final Type t = superType.getType();
                refs.add(t instanceof ReferenceType ? (ReferenceType)t : new ReferenceType(t));
            }
        }
        if ("?".equals(named)) {
            // make a WildcardType
            WildcardType wildcard = new WildcardType();
            if (refs.size() > 0) {
                final Type maybeSuper = refs.get(0).getType();
                if (maybeSuper instanceof ClassOrInterfaceType) {
                    String firstArgName = ((ClassOrInterfaceType)maybeSuper).getName();
                    // We accept [ super, SomeTypeHere ] with super as a named type (ClassOrInterfaceType),
                    // for ease of use during programmatic creation of wildcard bounds
                    if ("super".equals(firstArgName)) {
                        refs.remove(0);
                        if (refs.size() > 1) {
                            assert false : "A wildcard cannot extend more than one type; you sent `? super " + refs +"` to " + this;
                            throw new IllegalArgumentException("Only 1 argument allowed: " + refs);
                        }
                        wildcard.setSuper(new ReferenceType(refs.remove(0)));
                        return new TypeExpr(wildcard);
                    }
                }
                if (refs.size() > 1) {
                    assert false : "A wildcard cannot extend more than one type; you sent " + refs +" to " + this;
                    throw new IllegalArgumentException("Only 1 argument allowed: " + refs);
                }
                wildcard.setExtends(new ReferenceType(maybeSuper));
            }
            return new TypeExpr(wildcard);
        } else {
            if (named.endsWith(".class")) {
                named = named.substring(0, named.length() - 6);
            }
            primitiveCheck: {
                final Primitive primitive;
                switch (named) {
                    case "boolean":
                        primitive = Primitive.Boolean;
                        break;
                    case "byte":
                        primitive = Primitive.Byte;
                        break;
                    case "short":
                        primitive = Primitive.Short;
                        break;
                    case "int":
                        primitive = Primitive.Int;
                        break;
                    case "long":
                        primitive = Primitive.Long;
                        break;
                    case "float":
                        primitive = Primitive.Float;
                        break;
                    case "double":
                        primitive = Primitive.Double;
                        break;
                    default:
                        // skip the assertion and return statement below...
                        break primitiveCheck;
                }
                assert generics.length == 0 : "Primitives cannot have type arguments...";
                return new TypeExpr(new PrimitiveType(primitive));
            } //:primitiveCheck
            named = qualifyWellKnownType(named);
            final ClassOrInterfaceType rawType = new ClassOrInterfaceType(named);
            if (rawType.getTypeArguments() == null) {
                rawType.setTypeArguments(TypeArguments.withArguments(refs));
            } else if (!refs.isEmpty()){
                rawType.getTypeArguments().getTypeArguments().addAll(refs);
            }
            return new TypeExpr(rawType);
        }
    }

    default String qualifyWellKnownType(String named) {
        return WellKnownTypes.qualifyType(named);
    }

    default TypeParameter $typeParam(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression name, Expression ... generics) {
        String named = tools.resolveString(ctx, name, true);
        if (named.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a TypeParameter from empty name " + tools.debugNode(name));
        }

        final List<ClassOrInterfaceType> refs = new ArrayList<>(generics.length);
        TypeParameter parsed;
        try {
            // This allows you to send a single string with generics as one expression.
            parsed = JavaParser.parseTypeParameter(named);
        } catch (ParseException e) {
            throw X_Debug.rethrow(e);
        }
        if (X_Fu.isEmpty(generics)) {
            return parsed;
        }
        // when generics aren't empty, we'll want to parse each one separately,
        for (Expression generic : generics) {
            final Expression resolved = tools.resolveVar(ctx, generic);
            final String typeString = tools.resolveString(ctx, resolved, true);
            if (!typeString.isEmpty()) {
                final List<ClassOrInterfaceType> parsedChildren;
                try {
                    // We allow each argument to supply a list,
                    // as this may be a rather common way for expressions to multiplex
                    parsedChildren = JavaParser.parseTypeList(typeString);
                } catch (ParseException e) {
                    throw X_Debug.rethrow(e);
                }
                refs.addAll(parsedChildren);
            }
        }
        parsed.setTypeBound(refs);
        return parsed;
    }

    default AstMethodResult $replace(ApiGeneratorTools<Ctx> tools, Ctx ctx, @SystemProvided MethodCallExpr call,
                                     Expression pattern, Expression replacement) {
        assert call.getScope() instanceof MethodCallExpr :
            "A $replace call must be used after another MethodCallExpr,\n" +
            "For example: $range(1, 3, $n->`Hello$n`).$replace(\"Hello\", \"Hi\"\n" +
            "you sent: " + tools.debugNode(call);
        final MethodCallExpr scopeCall = (MethodCallExpr) call.getScope();
        final Maybe<AstMethodInvoker<Ctx>> scoped = findMethod(ctx, scopeCall);
        final AstMethodResult scopeValue = scoped.get().io(tools, ctx, scopeCall);
        final List<Expression> all = new ArrayList<>();

        final Expression resolvedPattern = tools.resolveVar(ctx, pattern);
        final String resolvedRegex = tools.resolveString(ctx, resolvedPattern);
        Pattern regex = Pattern.compile(resolvedRegex);

        final Expression resolvedReplacement = tools.resolveVar(ctx, replacement);
        final String replacementString = tools.resolveString(ctx, resolvedReplacement);

        for (Expression expression : scopeValue.getExprs()) {
            expression = tools.resolveVar(ctx, expression);
            String original = tools.resolveString(ctx, expression);
            final String newValue = regex.matcher(original).replaceAll(replacementString);
            all.add(templateLiteral(newValue));
        }
        final AstMethodResult result = new AstMethodResult(all);
        return result;
    }

    default AstMethodResult $remove(ApiGeneratorTools<Ctx> tools, Ctx ctx, @SystemProvided MethodCallExpr call,
                                     Expression pattern) {
        assert call.getScope() instanceof MethodCallExpr :
            "A $remove call must be used after another MethodCallExpr,\n" +
            "For example: $range(1, 3, $n->`Hello$n`).$remove(\"Hello2\")" +
            "you sent: " + tools.debugNode(call);
        final MethodCallExpr scopeCall = (MethodCallExpr) call.getScope();
        final Maybe<AstMethodInvoker<Ctx>> scoped = findMethod(ctx, scopeCall);
        final AstMethodResult scopeValue = scoped.get().io(tools, ctx, scopeCall);
        final List<Expression> all = new ArrayList<>();

        final Expression resolvedPattern = tools.resolveVar(ctx, pattern);
        final String resolvedRegex = tools.resolveString(ctx, resolvedPattern);
        Pattern regex = Pattern.compile(resolvedRegex);

        for (Expression expression : scopeValue.getExprs()) {
            expression = tools.resolveVar(ctx, expression);
            String original = tools.resolveString(ctx, expression);
            final String newValue = regex.matcher(original).replaceAll("");
            all.add(templateLiteral(newValue));
        }
        final AstMethodResult result = new AstMethodResult(all);
        return result;
    }

    @SuppressWarnings("unchecked")
    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx ctx, int end, LambdaExpr expr) {
        return $range(tools, ctx, 1, end, expr); // end is inclusive, so start at 1 (one indexed ranges)
    }

    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression end, LambdaExpr expr) {
        return $range(tools, ctx, 1, tools.extractInt(ctx, end), expr);
    }

    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx ctx, int start, int end, LambdaExpr expr) {
        final Statement body = new ReturnStmt(BooleanLiteralExpr.boolLiteral(true));
        final ArrayList<Parameter> params = new ArrayList<>();
        params.add(new Parameter(String.class, "$ignored"));
        final LambdaExpr alwaysTrue = new LambdaExpr(params, body, false);
        return $range(tools, ctx, start, end, alwaysTrue, expr);
    }

    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression start, Expression end, LambdaExpr expr) {
        return $range(tools, ctx, tools.extractInt(ctx, start), tools.extractInt(ctx, end), expr);
    }
    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx ctx, Expression start, Expression end, LambdaExpr filter, LambdaExpr expr) {
        return $range(tools, ctx, tools.extractInt(ctx, start), tools.extractInt(ctx, end), filter, expr);
    }

    default Iterable<Expression> $range(ApiGeneratorTools<Ctx> tools, Ctx context, int start, int end, LambdaExpr filter, LambdaExpr expr) {
        final List<Parameter> params = expr.getParameters();
        if (params.isEmpty() || params.size() > 1) {
            throw new IllegalArgumentException("$range lambdas must take exactly one argument; you sent " + expr);
        }
        final List<Parameter> filterParams = filter.getParameters();
        if (params.isEmpty() || params.size() > 1) {
            throw new IllegalArgumentException("$range lambda filters must take exactly one argument; you sent " + filterParams);
        }

        String varName = params.get(0).getId().getName();
        String filterName = filterParams.get(0).getId().getName();
        final Statement body = expr.getBody();
        final Statement filterBody = filter.getBody();
        ChainBuilder<Expression> chain = new ChainBuilder<>();
        int initial = start;
        StringTo<Do> undos = X_Collect.newStringMap(Do.class);
        for (;start<=end;start++) {
            // Wraps a clone of the lambda body in a SysExpr,
            // which includes listeners that will set/unset the named variable when executing the body.
            final SysExpr sysExpr = new SysExpr(immutable1(body.clone()));
            final int n = start;
            sysExpr.addGenericListener(null, ApiGeneratorContext.class, (vis, node, ctx)-> {
                final Do undo = ctx.addToContext(filterName, IntegerLiteralExpr.intLiteral(n));
                final boolean wasFirst = ctx.isFirstOfRange();

                ctx.setInRange(true);
                ctx.setFirstOfRange(n == initial);

                final Object result;
                if (tools.isConditionTrue(context, filterBody)) {
                    if (node instanceof Expression) {
                        node = tools.resolveVar(context, (Expression)node);
                    }
                    result = node.accept(vis, ctx);
                } else {
                    result = null;
                }

                ctx.setInRange(false);
                ctx.setFirstOfRange(wasFirst);

                undo.done();
                return result;
            });
            sysExpr.addUniversalListener(ApiGeneratorContext.class, (node, ctx)->{
                undos.getMaybe(varName).readIfPresent(Do::done);
                final Do undo = ctx.addToContext(varName, IntegerLiteralExpr.intLiteral(n));
                undos.put(varName, undo);
                ctx.setInRange(true);
                final boolean wasFirst = ctx.isFirstOfRange();
                ctx.setFirstOfRange(n == initial);

                return undo.doAfter(()->{
                    ctx.setFirstOfRange(wasFirst);
                    ctx.setInRange(false);
                });
            });
            sysExpr.addVoidListener(null, ApiGeneratorContext.class, (vis, node, ctx) -> {
                undos.getMaybe(filterName).readIfPresent(Do::done);
                final Do undo = ctx.addToContext(filterName, IntegerLiteralExpr.intLiteral(n));
                undos.put(filterName, undo);
                ctx.setInRange(true);
                final boolean wasFirst = ctx.isFirstOfRange();
                ctx.setFirstOfRange(n == initial);

                if (tools.isConditionTrue(context, filterBody)) {
                    node = tools.resolveNode(context, node);
                    node.accept(vis, ctx);
                }

                ctx.setFirstOfRange(wasFirst);
                ctx.setInRange(false);
                undo.done();
            });
            chain.add(sysExpr);
        }
        return chain;
    }

    default Maybe<In1Out1<MethodCallExpr, Object>> mapJavaMethod(Method method) {
        return ()->i->null;
    }

    default Maybe<AstMethodInvoker<Ctx>> findMethod(Ctx ctx, MethodCallExpr call) {

        String methodName = call.getName();
        ChainBuilder<Method> methods = new ChainBuilder<>();
        for (Method method : getClass().getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.getName().equals(methodName)) {
                methods.add(method);
            }
        }

        final List<Expression> astArgs = call.getArgs();
        final AstMethodInvoker<Ctx> bestInvoker = methods.build()
            .map(m -> {
                int posOfCtx = -1;
                int posOfTools = -1;
                int posOfMethod = -1;
                final Class<?>[] paramsTypes = m.getParameterTypes();
                final java.lang.reflect.Parameter[] params = m.getParameters();
                int adaptableSize = paramsTypes.length;
                for (int i = 0; i < paramsTypes.length; i++) {
                    if (ApiGeneratorContext.class.isAssignableFrom(paramsTypes[i])) {
                        assert posOfCtx == -1 : "Api Generator Methods should not have a type " +
                            "assignable to ApiGeneratorContext more than once in its arguments!\nYou Sent: " + m.toGenericString();
                        posOfCtx = i;
                        adaptableSize--;
                    }
                    if (ApiGeneratorTools.class.isAssignableFrom(paramsTypes[i])) {
                        assert posOfTools == -1 : "Api Generator Methods should not have a type " +
                            "assignable to ApiGeneratorTools more than once in its arguments!\nYou Sent: " + m.toGenericString();
                        posOfTools = i;
                        adaptableSize--;
                    }
                    if (MethodCallExpr.class == paramsTypes[i]) {
                        for (Annotation anno : m.getParameterAnnotations()[i]) {
                            if (anno.annotationType() == SystemProvided.class) {
                                posOfMethod = i;
                                adaptableSize--;
                            }
                        };
                    }
                }

                boolean vararg = paramsTypes.length > 0 &&
                        m.getParameters()[paramsTypes.length-1].isVarArgs();
                if (adaptableSize != astArgs.size()) {
                    if (!vararg) {
                        // argument length mismatch; bail.
                        return null;
                    }
                }
                // Now do type matching
                In1Out1<Expression, Object>[] mappers = new In1Out1[paramsTypes.length];
                for (int i = 0, astInd = 0; i < paramsTypes.length; i++) {
                    if (i == posOfCtx) {
                        mappers[i] = ignored -> ctx;
                    } else if (i == posOfTools) {
                        mappers[i] = ignored -> tools();
                    } else if (i == posOfMethod) {
                        mappers[i] = ignored -> call;
                    } else {
                        // Try to make an expression mapper.
                        if (vararg && i == paramsTypes.length-1) {
                            // The rest of the arguments are the vararg array...
                            final Class<?> type = paramsTypes[i];
                            int pos = i;
                            mappers[i] = expr->{
                                assert expr instanceof JsonContainerExpr : "Varargs calls must be wrapped in a JsonContainer";
                                JsonContainerExpr json = (JsonContainerExpr) expr;
                                int valSize = json.size();
                                Object vals = X_Fu.newArray(type.getComponentType(), valSize);
                                for (int o = 0, shift = 0; o < json.size(); o++) {
                                    final Expression node = json.getNode(o);
                                    final Expression resolved = tools().resolveVar(ctx, node);
                                    if (Boolean.TRUE.equals(resolved.getExtra("isWrappedVararg"))) {
                                        // A wrapped vararg value need to be stretched into the array...
                                        JsonContainerExpr items = (JsonContainerExpr) resolved;
                                        if (items.size() == 1) {
                                            final Expression item = items.getNode(0);
                                            Object val = adaptArgument(ctx, type, false, call, pos, item)
                                                .io(item);
                                            X_Fu.setValue(vals, o + shift, val);
                                        } else {
                                            valSize += items.size() - 1;
                                            final Object newVals = X_Fu.newArray(type.getComponentType(), valSize);
                                            System.arraycopy(vals, 0, newVals, 0, o);
                                            vals = newVals;
                                            for (int ind = 0; ind < items.size(); ind++) {
                                                Expression item = items.getNode(ind);
                                                item = tools().resolveVar(ctx, item);
                                                Object val = adaptArgument(ctx, type, false, call, pos, item)
                                                    .io(item);
                                                X_Fu.setValue(vals, o + shift++, val);
                                            }
                                        }
                                    } else {
                                        Object val = adaptArgument(ctx, type, false, call, pos, resolved)
                                            .io(resolved);
                                        X_Fu.setValue(vals, o + shift, val);
                                    }
                                }
                                return vals;
                            };
                        } else {
                            final Expression astArg = astArgs.get(astInd++);
                            mappers[i] = adaptArgument(ctx, m.getParameters()[i], call, i, astArg);
                        }
                    }
                }
                final Parameter[] parameters = Parameter.from(vararg, paramsTypes,
                    In1Out1.forEach(params, new String[params.length], java.lang.reflect.Parameter::getName)
                );
                AstMethodInvoker<Ctx> invoker = new AstMethodInvoker<>(ctx,
                    args->m.invoke(ApiGeneratorMethods.this, args),
                    mappers,
                    paramsTypes.length - adaptableSize,
                    parameters);

                return invoker;
            })
            .filter(Filter.ifNotNull())
            .reduce((was, is) -> was == null ? is : this.chooseBetter(ctx, was, is), null);
        return Maybe.immutable(bestInvoker);
    }

    default In1Out1<Expression,Object> adaptArgument(
        Ctx ctx,
        java.lang.reflect.Parameter param,
        MethodCallExpr call,
        int i, Expression astArg
    ) {
        return adaptArgument(ctx, param.getType(), param.isVarArgs(), call, i, astArg);
    }

    default In1Out1<Expression,Object> adaptArgument(
        Ctx ctx,
        Class<?> paramType,
        boolean varargs,
        MethodCallExpr call,
        int i, Expression astArg
    ) {
        if (paramType.isInstance(astArg)) {
            return ignored -> astArg;
        } else if (paramType == String.class) {
            return expr -> tools().resolveString(ctx, expr);
        } else if (paramType.isPrimitive()) {
            // The argument is not an expression that was supplied;
            // Lets try to deduce anything sane...
            // Ok... primitives can map to their ast counterparts
            if (paramType == int.class) {
                return expr -> tools().resolveInt(ctx, expr);
            }
        } else if (paramType.isAssignableFrom(astArg.getClass())) {
            return expr->expr;
        } else if (Node.class.isAssignableFrom(paramType)) {
            return expr-> {
                final Expression value = tools().resolveVar(ctx, expr);
                return value;
            };
        } else if (paramType.isArray()) {
            if (varargs) {
                List<In1Out1<Expression, Object>> results = new ArrayList<>();
                final List<Expression> args = call.getArgs();
                while (i < args.size()) {
                    final In1Out1<Expression, Object> result = adaptArgument(
                        ctx,
                        paramType.getComponentType(),
                        false,
                        call,
                        i,
                        astArg
                    );
                    results.add(result);
                    i++;
                }
            }
        } else if (HasSize.class.isAssignableFrom(paramType)) {

        }
        return unknown -> astArg; // TODO throw error instead?
    }

    default AstMethodInvoker<Ctx> chooseBetter(Ctx ctx, AstMethodInvoker<Ctx> was, AstMethodInvoker<Ctx> is) {
        return was == null ? is : was;
    }

}
