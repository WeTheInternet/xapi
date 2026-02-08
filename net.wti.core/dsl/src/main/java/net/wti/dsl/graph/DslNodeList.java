package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;

import java.util.List;

///
/// DslNodeList:
///
/// Runtime node representing a normalized ordered list of items.
///
/// This node is generally produced when analyzing values under a schema type like:
///  - {@code many(T1, T2, ...)} (list-ish with singleton lifting)
///  - other list-like constructs (future)
///
/// Items are modeled as {@link DslObject} so the list may contain either leaf values
/// (DslValue implementations) or nested nodes (DslNode implementations).
///
/// Ordering is preserved exactly as encountered/normalized during analysis.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 01:50
public interface DslNodeList extends DslNode {

    /**
     * @return the declared schema type that produced this list (often a DslTypeMany).
     */
    DslType getType();

    /**
     * @return ordered list items (values and/or nested nodes).
     */
    List<DslObject> getItems();
}
