package xapi.dev.ui.impl;

import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import xapi.fu.Do;
import xapi.fu.In2;
import xapi.fu.Mutable;
import xapi.fu.itr.CachingIterator.ReplayableIterable;
import xapi.fu.itr.MappedIterable;
import xapi.time.X_Time;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * A tool for creating a lazy parser which slurps up string values from ast.
 *
 * This does not do a full-visit up front, rather, it does a halting visit,
 * powering the iterable which returns strings.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/23/18 @ 4:18 AM.
 */
public class ExpressionSlurper {

    private static final String TERMINATOR = "\0";

    public static ReplayableIterable<String> slurpStrings(Class<?> logTo, Expression expression) {
        return slurpStrings(logTo, expression, 10_000, TimeUnit.MINUTES.toMillis(60));
    }

    public static ReplayableIterable<String> slurpStrings(Class<?> logTo, Expression expression, double waitTime, double lifespan) {
        final ComposableXapiVisitor<Object> visitor = ComposableXapiVisitor.onMissingFail(logTo);
        Mutable<String> next = new Mutable<>();
        // This entire pattern of "go through a possibly json container and slurp up lists of strings"
        // should be entirely generic, and move somewhere more reusable
        long ttl = lifespan <= 0 ? TimeUnit.DAYS.toMillis(7) : (long)Math.ceil(lifespan);
        Do pause = Do.unsafe(()->{
            synchronized (next) {
                next.wait(ttl);
            }
        });
        Do unpause = Do.unsafe(()->{
            synchronized (next) {
                next.notify();
            }
        });
        visitor.withJsonContainerTerminal((json, arg) -> {
            assert json.isArray() : "Only arrays may be used for imports!";
            json.getValues().forAll(Expression::accept, visitor, arg);
        })
            .withJsonPairRecurse(In2.ignoreAll())
            .withTemplateLiteralTerminal((tmp, arg) -> {
                next.set(tmp.getValueWithoutTicks());
                pause.done();
            })
            .withQualifiedNameTerminal((str, arg) -> {
                // no need to import unqualified names...
                next.set(str.getQualifiedName());
                pause.done();

            })
            .withStringLiteralTerminal((str, arg) -> {
                next.set(str.getValue());
                pause.done();
            });
        X_Time.runLater(()->{
            // Ugh, need X_Process to be able to schedule an interrupt...
            expression.accept(visitor, null);
            next.in(TERMINATOR);
        });

        return MappedIterable.mappedCaching(new Iterator<String>() {
            private String val;

            @Override
            @SuppressWarnings("StringEquality") // we mean to do this.  We send TERMINATOR to signal completion.
            public boolean hasNext() {
                if (val == null) {
                    val = next.block(waitTime);
                    if (val == TERMINATOR) {
                        val = null;
                    } else {
                        unpause.done();
                    }
                }
                return val != null;
            }

            @Override
            public String next() {
                final String v = val;
                val = null;
                return v;
            }
        });
    }
}
