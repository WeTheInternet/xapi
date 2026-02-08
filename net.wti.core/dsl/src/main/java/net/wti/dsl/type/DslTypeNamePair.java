package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeNamePair:
///
/// Schema/type object representing `namePair`.
/// Accepted runtime forms (later): "a:b" or { a: b } where both sides are <name>.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:47
public final class DslTypeNamePair extends ImmutableDslType {

    public DslTypeNamePair(MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
