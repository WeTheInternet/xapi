package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeInt:
///
/// Schema/type object representing `integer`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:47
public final class DslTypeInteger extends ImmutableDslType {

    public DslTypeInteger(MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
