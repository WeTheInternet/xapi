package net.wti.dsl.graph;

///
/// DslRootNode:
///
/// Root of a normalized DSL instance document.
///
/// This node is the top-level entrypoint returned by analysis/build pipelines.
/// It intentionally models a *single* root element, because the DSL instance
/// itself is expected to have one root tag (as declared by the schema).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 01:49
public interface DslRootNode extends DslNode {

    /**
     * @return the single root element node for this document.
     */
    DslNodeElement getRoot();

}
