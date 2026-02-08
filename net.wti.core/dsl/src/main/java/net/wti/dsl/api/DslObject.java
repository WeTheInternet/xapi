package net.wti.dsl.api;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

///
/// DslObject:
///
/// Base contract for anything derived from xapi AST.
/// Implementations should retain one-or-more source expressions for diagnostics,
/// annotation lookups, and template provenance.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 02:54
public interface DslObject {

    MappedIterable<Expression> getSourceAst();

}
