package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslTypeString:
///
/// Schema/type object representing `string`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 13:47
public class DslTypeString extends ImmutableDslType {

    public DslTypeString(final MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
