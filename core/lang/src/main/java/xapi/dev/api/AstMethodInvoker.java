package xapi.dev.api;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Mutable;
import xapi.fu.Rethrowable;
import xapi.fu.has.HasType;
import xapi.fu.itr.CachingIterator;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.EmptyIterator;

import static xapi.fu.itr.SingletonIterator.singleItem;

import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/6/16.
 */
public class AstMethodInvoker <Ctx extends ApiGeneratorContext<Ctx>> implements Rethrowable {

    protected static class AstMethodResult {
        boolean unfoldArrays;
        final Iterable<Expression> exprs;
        private final  Object actualResult;

        public AstMethodResult(Expression expr) {
            exprs = singleItem(expr);
            actualResult = expr;
        }

        public AstMethodResult(Iterable<Expression> expr) {
            exprs = expr;
            actualResult = expr;
        }

        public <Ctx extends ApiGeneratorContext<Ctx>> AstMethodResult(ApiGeneratorTools<Ctx> tools, Ctx ctx, Iterable<?> result) {
            actualResult = result;
            if (result instanceof HasType) {
                final Class<?> type = ((HasType) result).getType();
                if (Expression.class.isAssignableFrom(type)) {
                    // It's an array of expressions.  Use it directly.
                    exprs = (Iterable<Expression>) result;
                } else {
                    // It's an array of "something".  Wrap it in Json
                    ChainBuilder<Expression> res = Chain.startChain();
                    result.forEach(i->res.add(tools.boxResult(ctx, null, i)));
                    exprs = res;
                }
            } else {
                final CachingIterator<?> copy = CachingIterator.cachingIterator(result.iterator());
                if (!copy.hasNext()) {
                    exprs = EmptyIterator.none();
                } else {
                    if (copy.peek() instanceof Expression) {
                        if (copy.hasNoMore()) {
                            // only one expression. lets use it.
                            exprs = (Iterable<Expression>) result;
                        } else {
                            // Maybe more than one expression...
                            Mutable<Boolean> nonExpr = new Mutable<>(false);
                            copy.peekWhileTrue(item->{
                                if (item instanceof Expression) {
                                    return true;
                                }
                                nonExpr.in(true);
                                return false;
                            });
                            if (Boolean.FALSE.equals(nonExpr.out1())) {
                                exprs = (Iterable<Expression>) result;
                            } else {
                                ChainBuilder<Expression> newItems = Chain.startChain();
                                copy.forEachRemaining(item->newItems.add(tools.boxResult(ctx, null, item)));
                                exprs = newItems;
                            }
                        }
                    } else {
                        // It's an array of anythings... lets just box them all.
                        ChainBuilder<Expression> newItems = Chain.startChain();
                        copy.forEachRemaining(item->newItems.add(tools.boxResult(ctx, null, item)));
                        exprs = newItems;
                    }
                }
            }
        }

        public AstMethodResult(Object result, Expression expression) {
            exprs = singleItem(expression);
            actualResult = result;
        }

        public boolean isUnfoldArrays() {
            return unfoldArrays;
        }

        public void setUnfoldArrays(boolean unfoldArrays) {
            this.unfoldArrays = unfoldArrays;
        }

        public Iterable<Expression> getExprs() {
            return exprs;
        }

        public Object getActualResult() {
            return actualResult;
        }

        public static AstMethodResult none() {
            return new AstMethodResult(null, null, EmptyIterator.none());
        }
    }


    In1Out1<Object[], Object> invoker;
    private final In1Out1<Expression, Object>[] mappers;;
    private final Parameter[] params;
    private final int numSystem;

    public AstMethodInvoker(Ctx ctx, In1Out1Unsafe<Object[], Object> invoker, In1Out1<Expression, Object>[] mappers, int numSystem, Parameter ... params) {
        this.invoker = invoker;
        this.mappers = peekInit(ctx, invoker, mappers);
        this.params = params;
        this.numSystem = numSystem;
    }

    protected In1Out1<Expression, Object>[] peekInit(Ctx ctx, In1Out1Unsafe<Object[], Object> invoker, In1Out1<Expression, Object>[] mappers) {
        return mappers;
    }

    public AstMethodResult io(ApiGeneratorTools<Ctx> tools, Ctx ctx, MethodCallExpr expr) {

        if (invoker != null) {
            final Object[] args = extractArgs(tools, ctx, expr);
            final Object result = invoker.io(args);
            return expandResult(tools, ctx, expr, args, result);
        }
        return new AstMethodResult(expr);
    }

    protected AstMethodResult expandResult(ApiGeneratorTools<Ctx> tools, Ctx ctx, MethodCallExpr expr, Object[] args, Object result) {
        if (result == null) {
            // Could not find a result; leave the method call unharmed
            return new AstMethodResult(expr);
        }
        if (result instanceof AstMethodResult) {
            return (AstMethodResult) result;
        }
        if (result instanceof Expression) {
            return new AstMethodResult((Expression)result);
        }
        if (result instanceof Iterable) {
            return new AstMethodResult(tools, ctx, (Iterable<?>)result);
        }
        return new AstMethodResult(result, tools.boxResult(ctx, expr, result));

    }

    protected AstMethodResult expandArgument(
        ApiGeneratorTools<Ctx> tools,
        Ctx ctx,
        MethodCallExpr expr,
        In1Out1<Expression, Object> mapper,
        Expression argument
    ) {
        if (mapper != null) {
            final Object mapped = mapper.io(argument);
            if (mapped != null) {
                final AstMethodResult result = expandResult(tools, ctx, expr, new Object[]{argument}, mapped);
                for (Expression item : result.getExprs()) {
                    item.borrowExtras(argument);
                }
                return result;
            }
        }
        if (argument == null) {
            // Could not find a result; leave the method call unharmed
            return AstMethodResult.none();
        }

        // Could not find a result; leave the method call unharmed
        return new AstMethodResult(expr);

    }

    private Object[] extractArgs(ApiGeneratorTools<Ctx> tools, Ctx ctx, MethodCallExpr expr) {
        final List<Expression> args = expr.getArgs();
        int totalArgs = mappers.length, shift = 0;
        Object[] result = new Object[totalArgs];
        int system = numSystem;
        while (system --> 0) {
            totalArgs--;
            result[shift] = mappers[shift].io(null);
            shift++;
        }
        boolean hasVarargs = params.length > 0 && params[params.length-1].isVarArgs();
        for (int i = shift; i < result.length; i++ ) {
            Expression arg;
            if (hasVarargs && i == result.length-1) {
                // There are varargs, we need to encapsulate them in an array...
                arg = new JsonContainerExpr(args.subList(i-shift, args.size()));
                arg.addExtra("isWrappedVararg", true);
            } else {
                arg = args.get(i-shift);
            }
            final AstMethodResult astResult = expandArgument(tools, ctx, expr, mappers[i], arg);
            result[i] = astResult.actualResult;
        }
        return result;
    }

}
