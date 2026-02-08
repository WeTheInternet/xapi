package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeName:
///
/// Schema/type object representing `name` (a Java identifier).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:45
public final class DslTypeName extends ImmutableDslType {

    public DslTypeName(MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
