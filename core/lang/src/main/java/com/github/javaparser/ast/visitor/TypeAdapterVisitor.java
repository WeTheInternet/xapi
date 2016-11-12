package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.visitor.TypeAdapterVisitor.TypeAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/6/16.
 */
public class TypeAdapterVisitor <A> extends GenericVisitorAdapter<TypeAdapter, A> {

    class TypeAdapter {
        ClassTo<In2Out1<Node, A, ?>> adapters = X_Collect.newClassMap();

        public boolean isAdaptable(Class<?> type) {
            for (In2Out1<Node, A, ?> o : adapters.iterateAssignableKey(type)) {
                return true;
            }
            return false;
        }

        public In2Out1<Node, A, ?> adapter(Class<?> cls) {
            final In2Out1<Node, A, ?> adapter = adapters.getAssignable(cls);
            return adapter == null ? In2Out1.returnNull() : adapter;
        }

        public <T, N extends Node> TypeAdapter addAdapter(Class<? super T> typeClass, In2Out1<N, A, T> mapper) {
            adapters.put(typeClass, (In2Out1<Node, A, ?>) mapper);
            return this;
        }

        public <T, N extends Node> TypeAdapter addAdapter(Class<? super T> typeClass, In1Out1<N, T> mapper) {
            adapters.put(typeClass, (n, a)->mapper.io((N)n));
            return this;
        }
    }

    @Override
    public TypeAdapter visit(IntegerLiteralExpr n, A arg) {
        return new TypeAdapter()
            .addAdapter(int.class, IntegerLiteralExpr::intValue)
            .addAdapter(String.class, IntegerLiteralExpr::getValue);
    }

}
