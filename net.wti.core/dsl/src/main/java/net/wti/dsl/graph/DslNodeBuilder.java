package net.wti.dsl.graph;

///
/// DslNodeBuilder:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:10
public interface DslNodeBuilder<T extends DslNode> extends DslNode {

    /**
     * Freeze the builder into an immutable snapshot node.
     *
     * Implementations should:
     *  - validate builder invariants where appropriate,
     *  - defensively copy and/or wrap internal collections to prevent mutation after freezing.
     */
    T buildImmutable();
}
