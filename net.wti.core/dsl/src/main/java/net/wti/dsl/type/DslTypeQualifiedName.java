package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeQualifiedName:
///
/// Schema/type object representing `qualifiedName` (dot-separated identifiers).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:46
public final class DslTypeQualifiedName extends ImmutableDslType {

    public DslTypeQualifiedName(MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
