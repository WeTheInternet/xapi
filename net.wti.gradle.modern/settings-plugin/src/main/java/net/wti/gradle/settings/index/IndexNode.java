package net.wti.gradle.settings.index;

import net.wti.gradle.settings.api.ModuleIdentity;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.data.MultiSet;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Objects;
import java.util.TreeMap;

/**
 * IndexNode:
 * <p>
 * <p>Represents a single project:platform:module.
 * <p>
 * <p>Each node records all outgoing and incoming edges,
 * <p>as well as state about a given module, to determine its "liveness".
 * <p>
 * <p>This graph is then used to collapse empty modules, but still retain dependency transitivity.
 * <p>We create an IndexNode for every possible project:platform:module, but only those nodes which are deemed to be
 * "live" will become gradle projects at runtime. To be considered live, a p:p:m module must either:
 * <ol>
 *     <li>Contain files in src/moduleName/*</li>
 *     <li>Contain non-default changes to modules/moduleName.gradle</li>
 *     <li>Contain explicit external / project dependencies</li>
 *     <li>Be the target of another module's explicit project dependency</li>
 *     <li>Contain alwaysLive=true in schema.xapi</li>
 * </ol>
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 09/06/2024 @ 11:12 p.m.
 */
public class IndexNode {

    private final EnumSet<LivenessReason> livenessReasons = EnumSet.noneOf(LivenessReason.class);

    private final SetLike<IndexNode> includes;
    private final SetLike<IndexNode> requires;
    private final Lazy<SetLike<IndexNode>> allDependencies;
    private final Lazy<SetLike<IndexNode>> compressedDependencies;
    private final SetLike<IndexNode> outgoingDependencies;
    private final ModuleIdentity identity;

    private boolean resolved, allowCompress = true;
    private volatile Integer compressed;
    private static final In1Out1<IndexNode,SetLike<IndexNode>> allDependenciesMapper = IndexNode::allDependencies;
    private static final In1Out1<IndexNode,SetLike<IndexNode>> compressedDependenciesMapper = IndexNode::compressedDependencies;
    private static final In2Out1<SetLike<IndexNode>, IndexNode, Boolean> setLikeContains = SetLike::contains;
    private static final In2Out1<IndexNode, IndexNode, Boolean> isRequirementFilter = IndexNode::isRequirement;
    private boolean deleted;

    private boolean isRequirement(IndexNode node) {
        return requires.contains(node) || requires.map(allDependenciesMapper).anyMatch(setLikeContains, node);
    }

    public IndexNode(ModuleIdentity identity) {
        this.identity = identity;
        requires = X_Jdk.setLinkedSynchronized();
        includes = X_Jdk.setLinkedSynchronized();
        allDependencies = Lazy.deferred1(allDependenciesMapper.supply(this));
        compressedDependencies = Lazy.deferred1(allDependenciesMapper.supply(this));
        outgoingDependencies = X_Jdk.setLinkedSynchronized();
    }

    public void include(IndexNode other) {
        assert this != other : "Cannot include yourself";
        if (resolved) {
            throw new IllegalStateException("Cannot add node " + other + " to " + this + " as this node is already resolved");
        }
        includes.add(other);
        other.outgoingDependencies.add(this);
    }

    public void require(IndexNode other) {
        assert this != other : "Cannot require yourself";
        requires.add(other);
        other.outgoingDependencies.add(this);
    }

    private SetLike<IndexNode> compressedDependencies() {
        final SetLike<IndexNode> graduates = X_Jdk.setLinked();
        final MultiSet<Integer, IndexNode> sortedCandidates = X_Jdk.toMultiSet(new TreeMap<>(
                // use a reverse-ordered integer-keyed treemap to store insertion-ordered sets of nodes
                // we'll use the number of dependencies as key, so we process 'bigger' nodes first,
                // in the hopes of skipping more of the smaller nodes.
                // We should probably figure out how to sort by platform, then by size...
                // Perhaps multiplying by platform-depth ^ 2?
                (a,b)-> -a.compareTo(b)), k->X_Jdk.setLinked()
        );
        // never compress a requires.
        graduates.addNow(requires);

        // add the includes into our sorted multiset
        for (IndexNode included : allIncluded()) {
            sortedCandidates.get(included.getAllDependencies().size()).add(included);
        }
        // flatten the multiset into IndexNode ordered by the total number of includes per dependencies, descending.
        final IndexNode[] sortedIncludes = sortedCandidates.flatten().toArray(IndexNode.class);
        // in order to reduce an n^2 operation to "the best log(n) possible", we'll use a BitSet to store 'is already included' state
        final BitSet skipSet = new BitSet();
        for (int i = 0; i < sortedIncludes.length; i++) {
            if (skipSet.get(i)) {
                continue;
            }
            IndexNode candidate = sortedIncludes[i];
            if (requires.contains(candidate)) {
                continue;
            }
            if (requires.map(allDependenciesMapper).anyMatch(setLikeContains, candidate)) {
                // one of our requires already includes this dependency. Ignore it.
                continue;
            }
            // this node was not contained by any of our requires, so it will graduate
            graduates.add(candidate);
            // now, any of our includes that our candidate also includes can be erased.
            final SetLike<IndexNode> candidateDependencies = candidate.getAllDependencies();
            for (int j = i; j < sortedIncludes.length; j++) {
                final IndexNode check = sortedIncludes[j];
                if (candidateDependencies.contains(check)) {
                    skipSet.set(j); // no need to include j, it'll already be included by candidate
                } else if (candidate.requires.anyMatch(isRequirementFilter, check)) {
                    skipSet.set(j); // no need to include j, it'll already be included by candidate
                }
            }
        }
        return graduates;

    }

    protected SetLike<IndexNode> allIncluded() {
        if (!allowCompress) {
            return includes;
        }
        SetLike<IndexNode> allChildren = X_Jdk.setLinked();
        for (IndexNode include : includes) {
            if (include.isLive()) {
                allChildren.add(include);
            } else {
                include.setDeleted(true);
                for (IndexNode grandkid : include.allIncluded()) {
                    allChildren.add(grandkid);
                }
            }
        }
        return allChildren;
    }
    public SetLike<IndexNode> getAllDependencies() {
        return allDependencies.out1();
    }
    public SetLike<IndexNode> getCompressedDependencies() {
        return compressedDependencies.out1();
    }

    public SetLike<IndexNode> getOutgoingDependencies() {
        return outgoingDependencies;
    }

    private SetLike<IndexNode> allDependencies() {
        SetLike<IndexNode> allChildren = X_Jdk.setLinked();
        for (IndexNode include : requires.plus(includes)) {
            if (include == this) {
                throwDependencyCycle(this, include);
            }
            if (include.isLive()) {
                allChildren.add(include);
            } else {
                include.setDeleted(true);
                for (IndexNode grandkid : include.getAllDependencies()) {
                    if (grandkid == this) {
                        throwDependencyCycle(this, grandkid);
                    }
                    allChildren.add(grandkid);
                }
            }
        }
        return allChildren;
    }

    private void throwDependencyCycle(final IndexNode nodeA, final IndexNode nodeB) {
        // TODO: actually build up a visual representation of the cycle, from nodeA to nodeB
        throw new IllegalStateException("Dependency cycle detected from " + nodeA + " -> " + nodeB);
    }

    public boolean isLive() {
        if (deleted) {
            return false;
        }
        if (livenessReasons.isEmpty()) {
            return false;
        }
        if (livenessReasons.size() == 1) {
            if (livenessReasons.contains(LivenessReason.has_includes)) {
                // the only reason this module is live is that is has_includes
                // if this module has nobody live including it, it should not be considered live...
                // but we currently don't know the outgoing edges yet (we could, it's just not done) :-/

                for (IndexNode include : includes) {
                    if (include.isLive()) {
                        // something we include is actually live, we'll stay live for now.
                        return true;
                    }
                }
                // we are an include that includes things that ultimately depend on nothing. prune this node.
                livenessReasons.clear();
                return false;
            }
        }
        return true;
    }
    public boolean addLiveness(LivenessReason reason) {
        return livenessReasons.add(reason);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IndexNode indexNode = (IndexNode) o;
        return Objects.equals(identity, indexNode.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }

    @Override
    public String toString() {
        return identity.toString() + '{' +
            "includes: " + includes.size() + ", " +
            "requires: " + includes.size() + ", " +
            "liveness: " + livenessReasons +
        '}';
    }

    public EnumSet<LivenessReason> getLivenessReasons() {
        return livenessReasons;
    }

    public ModuleIdentity getIdentity() {
        return identity;
    }

    public boolean removeOutgoing(final IndexNode node) {
        return outgoingDependencies.remove(node);
    }

    public boolean isIncludeOnly() {
        return livenessReasons.size() == 1 && livenessReasons.contains(LivenessReason.has_includes);
    }

    public boolean hasOutgoing() {
        return outgoingDependencies.isNotEmpty();
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
