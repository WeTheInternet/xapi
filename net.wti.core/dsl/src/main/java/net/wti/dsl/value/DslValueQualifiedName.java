package net.wti.dsl.value;

import net.wti.dsl.type.DslTypeQualifiedName;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslValueQualifiedName:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 16:20
public final class DslValueQualifiedName implements DslValue<String> {

    private final MappedIterable<Expression> sourceAst;
    private final DslTypeQualifiedName type;
    private final String value;

    public DslValueQualifiedName(MappedIterable<Expression> sourceAst, DslTypeQualifiedName type, String value) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
    }

    @Override
    public String getDslValue() {
        return value;
    }

    @Override
    public DslTypeQualifiedName getType() {
        return type;
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
