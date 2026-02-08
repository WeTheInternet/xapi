package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslTypeOne:
///
/// one(T1, T2, ...): exactly one value; multiple args imply type union.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:48
public final class DslTypeOne extends ImmutableDslType {

    private final List<DslType> choices;

    public DslTypeOne(MappedIterable<Expression> sourceAst, List<? extends DslType> choices) {
        super(sourceAst);
        Objects.requireNonNull(choices, "choices must not be null");
        this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
    }

    public List<DslType> getChoices() {
        return choices;
    }
}
