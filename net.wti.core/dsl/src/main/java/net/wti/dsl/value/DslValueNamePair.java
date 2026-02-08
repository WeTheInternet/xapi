package net.wti.dsl.value;

import net.wti.dsl.type.DslTypeNamePair;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.Out2;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslValueNamePair:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 16:21
public final class DslValueNamePair implements DslValue<Out2<String, String>> {

    private final MappedIterable<Expression> sourceAst;
    private final DslTypeNamePair type;
    private final Out2<String, String> value;

    public DslValueNamePair(MappedIterable<Expression> sourceAst, DslTypeNamePair type, Out2<String, String> value) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
    }

    @Override
    public Out2<String, String> getDslValue() {
        return value;
    }

    @Override
    public DslTypeNamePair getType() {
        return type;
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
