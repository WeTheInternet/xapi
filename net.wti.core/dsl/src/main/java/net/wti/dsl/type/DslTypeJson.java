package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslTypeJson:
///
/// Schema/type object representing `json` / `<json />`.
/// This is intentionally permissive at schema-time; validation/normalization rules
/// should be enforced by the analyzer/build pipeline as needed.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 01:42
public final class DslTypeJson extends ImmutableDslType {

    public DslTypeJson(final MappedIterable<Expression> sourceAst) {
        super(sourceAst);
    }
}
