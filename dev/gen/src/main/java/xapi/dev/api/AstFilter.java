package xapi.dev.api;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.string.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/25/16.
 */
public interface AstFilter <T> extends Filter1<T> {

    static <T extends Expression, Ctx extends ApiGeneratorContext<Ctx>> AstFilter <T> matchNodeSource(
        // You must supply the context at creation,
        // so we can close over it and return an argument that only takes T
        Ctx ctx,
        // The ast describing the filter.
        Expression filterExpression,
        In2Out1<Ctx, Expression, T> executor) {


        final Out1<String> filterResult = executor.supply1(ctx).supply(filterExpression).map(Expression::toSource);
        return In2Out1.with1Deferred(X_String::equalIgnoreWhitespace, filterResult)
            .mapIn(In1Out1.<T>identity().mapOut(Expression::toSource))::io;

    }
    interface F {
        static Boolean f(Integer i, Integer j) {
            return i == j;
        }
    }
    static <T, Ctx extends ApiGeneratorContext<Ctx>> AstFilter <T> matchInt(
        Ctx ctx,
        Expression filterExpression,
        In2Out1<Ctx, Expression, Integer> executor,
        In1Out1<T, Integer> getter) {

        final Out1<Integer> filterResult = executor.supply1(ctx).supply(filterExpression);
        In2Out1<Integer, Integer, Boolean> intEquals = Integer::equals;
        return In2Out1.with1Deferred(intEquals, filterResult)
                .mapIn(getter)::io;
    }

    static <T> AstFilter <T> matchObject(T value) {
        return value::equals; // null check
    }

    static <Ctx extends ApiGeneratorContext<Ctx>> AstFilter <Expression> matchBoolean(Ctx ctx, In2Out1<Ctx, Expression, Expression> getter) {
        return getter.supply1(ctx).mapOut(e->(BooleanLiteralExpr)e).mapOut(BooleanLiteralExpr::getValue)::io;
    }

    static <T> AstFilter <T> matchString(String value, In1Out1<T, String> getter) {
        return getter.mapOut(
                             // null check and String interner optimization
            result -> result == value
                // let the foreign object dictate equality
                || result.equals(value)
        )::io;
    }

}
