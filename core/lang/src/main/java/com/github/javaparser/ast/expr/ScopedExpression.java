package com.github.javaparser.ast.expr;

import xapi.fu.ReturnSelf;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/21/17.
 */
public interface ScopedExpression <Self extends Expression & ScopedExpression<Self>>
    extends ReturnSelf<Self> {

    Expression getScope();

    default Expression getRootScope() {
        Expression next, rootMost = self();
        while (
            (next =
                rootMost instanceof ScopedExpression
                    ? ((ScopedExpression<?>)rootMost).getScope()
                    : rootMost) != null) {
            if (rootMost == next) {
                return rootMost;
            }
            rootMost = next;
        }
        return rootMost;
    }

    void setScope(Expression scope);
}
