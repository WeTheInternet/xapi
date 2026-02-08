package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslTypeRef:
///
/// Schema/type object representing `typeRef("aliasName")`.
/// This is a *schema-time* indirection; actual resolution happens in the analyzer/build pipeline.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:41
public final class DslTypeRef extends ImmutableDslType {

    private final String aliasName;

    public DslTypeRef(final MappedIterable<Expression> sourceAst, final String aliasName) {
        super(sourceAst);
        this.aliasName = Objects.requireNonNull(aliasName, "aliasName must not be null");
        if (aliasName.isEmpty()) {
            throw new IllegalArgumentException("aliasName must not be empty");
        }
    }

    public String getAliasName() {
        return aliasName;
    }
}
