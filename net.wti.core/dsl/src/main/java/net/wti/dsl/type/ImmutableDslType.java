package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// AbstractDslType:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:38
public class ImmutableDslType implements DslType {

    private final MappedIterable<Expression> sourceAst;

    public ImmutableDslType(MappedIterable<Expression> sourceAst) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
    }

    @Override
    public MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }

}
