package net.wti.dsl.value;

import net.wti.dsl.type.DslTypeBoolean;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslValueBoolean:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 16:19
public final class DslValueBoolean implements DslValue<Boolean> {

    private final MappedIterable<Expression> sourceAst;
    private final DslTypeBoolean type;
    private final Boolean value;

    public DslValueBoolean(MappedIterable<Expression> sourceAst, DslTypeBoolean type, Boolean value) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
    }

    @Override
    public Boolean getDslValue() {
        return value;
    }

    @Override
    public DslTypeBoolean getType() {
        return type;
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
