package net.wti.dsl.value;

import net.wti.dsl.type.DslTypeString;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslValueString:
///
/// Runtime value of type `string`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:39
public final class DslValueString implements DslValue<String> {

    private final MappedIterable<Expression> sourceAst;
    private final DslTypeString type;
    private final String value;

    public DslValueString(MappedIterable<Expression> sourceAst, DslTypeString type, String value) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value; // allow null? decide later; keeping permissive for now
    }

    @Override
    public String getDslValue() {
        return value;
    }

    @Override
    public DslTypeString getType() {
        return type;
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
