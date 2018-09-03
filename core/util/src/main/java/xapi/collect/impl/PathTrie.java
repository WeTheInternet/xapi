package xapi.collect.impl;

import xapi.collect.api.PrefixedMap;
import xapi.fu.Lazy;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/15/17.
 */
public class PathTrie <T> {

    public static abstract class PathNode<T> {

        public abstract T getValue();

    }

    public static class ValueNode<T> extends PathNode<T> {

        protected volatile T value;

        @Override
        public T getValue() {
            return value;
        }

        public ValueNode setValue(T value) {
            this.value = value;
            return this;
        }
    }

    public class BranchNode extends PathNode<T> {

        protected final Lazy<PrefixedMap<PathNode<T>>> children = Lazy.deferred1(newFactory(PathNode.class));
        protected volatile ValueNode<T> value;
        protected volatile WildcardNode wildcard;

        @Override
        public T getValue() {
            return value == null ? null : value.getValue();
        }

        public WildcardNode getWildcard() {
            return wildcard;
        }

        public WildcardNode getOrCreateWildcard() {
            synchronized (children) {
                if (wildcard == null) {
                    wildcard = new WildcardNode();
                }
            }
            return wildcard;
        }
    }

    /**
     * A wildcard node may be of the form * or ** or both.
     *
     * At wildcard nodes, we switch from prefix matching to suffix matching.
     * In addition, * wildcards will stop matching after reaching {@link PathTrie#getDelimiter()}.
     *
     */
    public class WildcardNode extends PathNode<T> {
        protected final Lazy<PrefixedMap<BranchNode>> delimited = Lazy.deferred1(newFactory(BranchNode.class));
        protected final Lazy<PrefixedMap<BranchNode>> greedy = Lazy.deferred1(newFactory(BranchNode.class));

        @Override
        public T getValue() {
            if (delimited.isResolved()) {
                T val = getValue(delimited, "");
                if (val != null) {
                    return val;
                }
            }
            if (greedy.isResolved()) {
                T val = getValue(greedy, "");
                if (val != null) {
                    return val;
                }
            }
            return null;
        }

        protected T getValue(Lazy<PrefixedMap<BranchNode>> delimited, String s) {
            if (delimited.isUnresolved()) {
                return null;
            }
            final PrefixedMap<BranchNode> map = delimited.out1();
            while (s != null){
                for (BranchNode node : map.findPrefixed(s)) {
                    final T val = node.getValue();
                    if (val != null) {
                        return val;
                    }
                }
                if (s.isEmpty()) {
                    return null;
                } else {
                    s = s.substring(1);
                }
            }
            return null;
        }
    }

    protected <P extends PathNode, G extends P> Out1<PrefixedMap<P>> newFactory(Class<G> nodeType) {
        return Out1.class.cast(factory);
    }

    private final BranchNode root;
    private final Out1<PrefixedMap> factory;

    /**
     * Global wildcard node, /**
     */
    private WildcardNode wildcard;

    public PathTrie() {
        this(MultithreadedStringTrie::new);
    }


    public PathTrie(Out1<PrefixedMap> source) {
        this.factory = source;
        this.root = createRoot();
    }

    private BranchNode createRoot() {
        return new BranchNode();
    }

    public String getDelimiter() {
        return "/";
    }

    public ValueNode addPath(String path, T value) {

        final String delim = getDelimiter();
        int nextStar = path.indexOf('*');
        if (nextStar == -1) {
            // static path string, we can just add it directly...


        } else {
            // there is a wildcard.  Add a PathNode with a branch
            if (this.wildcard == null) {

            }
        }
        return null;
    }


}
