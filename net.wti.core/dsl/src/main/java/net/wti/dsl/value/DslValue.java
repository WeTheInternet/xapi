package net.wti.dsl.value;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;

///
/// DslValue:
///
/// Marker for normalized values produced from xapi AST under control of a DslType.
/// Implementations should generally be immutable and retain source AST via DslObject.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 02:59
public interface DslValue <T> extends DslObject {

    T getDslValue();

    DslType getType();

}
