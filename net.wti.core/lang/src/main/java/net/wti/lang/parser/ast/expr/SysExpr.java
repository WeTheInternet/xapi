package net.wti.lang.parser.ast.expr;

import net.wti.lang.parser.ast.Node;
import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.ModifierVisitorAdapter;
import net.wti.lang.parser.ast.visitor.VoidVisitor;
import xapi.fu.*;
import xapi.fu.Filter.Filter1;
import xapi.fu.Filter.Filter2;
import xapi.fu.itr.CountedIterator;
import xapi.fu.itr.MappedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/16.
 */
public class SysExpr extends Expression {

    public interface GenericFactory extends
        In2Out1<
            GenericVisitor, Object, In3Out1<GenericVisitor, Node, Object, Object>
        >
    { }

    public interface VoidFactory extends
        In2Out1<
            VoidVisitor, Object, In3<VoidVisitor, Node, Object>
        >
    { }

    private final Out1<? extends Node>[] nodes;
    private GenericFactory genericFactory;
    private In2Out1<Node, Object, Do.Closeable> universalListener;

    private VoidFactory voidFactory;

    public SysExpr(Out1<? extends Node> ... fromNodes) {
        nodes = fromNodes;
        universalListener = (n, o)->Do.NOTHING;
        genericFactory =
            (vis, arg)->
                (v, node, argument)->
                    node.accept(vis, argument);
        voidFactory = (vis, arg)->
            (v, node, argument)->
                node.accept(vis, argument);
    }

    public SysExpr(Iterable<Node> fromNodes) {
        this(unroll(fromNodes));
    }

    private static Out1<Node>[] unroll(Iterable<Node> fromNodes) {
        final Out1<Node>[] results;
        final CountedIterator<Node> itr = CountedIterator.count(fromNodes);
        results = new Out1[itr.size()];
        int i = 0;
        for (Node node : fromNodes) {
            results[i++] = Immutable.immutable1(node);
        }
        return results;
    }

    public <R, A, T extends GenericVisitor<R, A>, VisType extends T, ArgType extends A> void addGenericListener(
                Class<VisType> visitorType,
                Class<ArgType> argType,
                In3Out1<T, Node, A, R> listener
        ) {
        addGenericListener((vis, arg)->
            ( visitorType == null || visitorType.isInstance(vis))
                    && (argType == null || argType.isInstance(arg)),
            listener
        );
    }

    public <R, A, T extends GenericVisitor<R, A>> void addGenericListener(
                Filter2<Object, T, A> filter,
                In3Out1<T, Node, A, R> listener
        ) {
        final GenericFactory was = genericFactory;
        genericFactory = (vis, arg) -> {
            if (filter.filter(vis, arg)) {
                    return (In3Out1)listener;
            }
            return was.io(vis, arg);
        };
    }

    public <A, T extends VoidVisitor<A>, VisType extends T, ArgType extends A> void addVoidListener(
                Class<VisType> visitorType,
                Class<ArgType> argType,
                In3<T, Node, A> listener
        ) {
        addVoidListener((vis, arg)->
            ( visitorType == null || visitorType.isInstance(vis))
                    && (argType == null || argType.isInstance(arg)),
            listener
        );
    }

    public <A, T extends VoidVisitor<A>> void addVoidListener(
                Filter2<Object, T, A> filter,
                In3<T, Node, A> listener
        ) {
        final VoidFactory was = voidFactory;
        voidFactory = (vis, arg) -> {
            if (filter.filter(vis, arg)) {
                    return (In3)listener;
            }
            return was.io(vis, arg);
        };
    }

    @Override
    @SuppressWarnings("unchecked") // we check listeners on the way in.
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    public <A, R, Vis extends GenericVisitor<R, A>> In3Out1<Vis, Node, A, R> genericVisit(Vis vis, A arg) {
        final In3Out1 factory = genericFactory.io(vis, arg);
        return (v, n, a) -> {
            try (
                Do.Closeable undo = universalListener.io(n, a)
            ){
                return (R)factory.io(v, n, a);
            }
        };
    }

    public <A, Vis extends VoidVisitor<A>> In3<Vis, Node, A> voidVisit(Vis vis, A arg) {
        final In3 factory = voidFactory.io(vis, arg);
        return (v, n, a) -> {
            try (
                Do.Closeable undo = universalListener.io(n, a)
            ) {
                factory.in(v, n, a);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked") // we check listeners on the way in.
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public <A, S extends A> SysExpr addUniversalListener(Class<S> argType, In2Out1<Node, A, Do.Closeable> callback) {
        final In2Out1<Node, Object, Do.Closeable> was = universalListener;
        universalListener = (node, arg) -> {
            final Do.Closeable toUndo = was.io(node, arg);
            if (argType.isInstance(arg) || arg == null) {
                final Do.Closeable undo = callback.io(node, (A) arg);
                return toUndo.doAfter(undo).closeable();
            }
            return toUndo;
        };
        return this;
    }

    public MappedIterable<Node> nodes() {
        return MappedIterable.mapped(nodes).map(Out1::out1);
    }

    public <A, R> R readAsOneNode(In2Out1<Node, A, R> callback, A arg) {
        if (nodes.length == 1) {
            final Node node = nodes[0].out1();
        try (
            Do.Closeable undo = universalListener.io(node, arg)
            ) {
                R r = callback.io(node, arg);
                return r;
            }
        }
        JsonContainerExpr json = JsonContainerExpr.jsonArray(nodes().map(n->(Expression)n));
        try (
            Do.Closeable undo = universalListener.io(json, arg)
        ) {
            R r = callback.io(json, arg);
            return r;
        }
    }

    public <A> SysExpr readAllNodes(In1<Node> callback, A arg) {
        return readNodesWhile(Filter1.TRUE, callback, arg);
    }
    public <A> SysExpr readNodesWhile(Filter1<Node> filter, In1<Node> callback, A arg) {
        for (Out1<? extends Node> node : nodes) {
            final Node n = node.out1();
            if (filter.filter1(n)) {
                try (
                    final Do.Closeable undo = universalListener.io(n, arg)
                ){
                    callback.in(n);
                }
            } else {
                return this;
            }
        }
        return this;
    }

    public void shareState(SysExpr expr) {
        expr.genericFactory = genericFactory;
        expr.voidFactory = voidFactory;
        expr.universalListener = universalListener;
    }

    public <A> void modify(ModifierVisitorAdapter a, A arg) {
        final In3Out1<GenericVisitor, Node, Object, Object> mapper = genericFactory.io(a, arg);
        for (int i = 0; i < nodes.length; i++) {
            final Object result = mapper.io(a, nodes[i].out1(), arg);
            nodes[i] = Immutable.immutable1((Node)result);
        }
    }
}
