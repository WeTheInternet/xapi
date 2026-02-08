package net.wti.dsl.value;

import net.wti.dsl.type.DslTypeInteger;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslValueInteger:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 16:19
public final class DslValueInteger implements DslValue<Integer> {

    private final MappedIterable<Expression> sourceAst;
    private final DslTypeInteger type;
    private final Integer value;

    public DslValueInteger(MappedIterable<Expression> sourceAst, DslTypeInteger type, Integer value) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
    }

    @Override
    public Integer getDslValue() {
        return value;
    }

    @Override
    public DslTypeInteger getType() {
        return type;
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
