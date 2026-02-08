package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeBoolean:
///
/// Schema/type object representing `boolean`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:46
public final class DslTypeBoolean extends ImmutableDslType {

    public DslTypeBoolean(MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
