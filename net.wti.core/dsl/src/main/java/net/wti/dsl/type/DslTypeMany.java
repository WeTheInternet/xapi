package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslTypeMany:
///
/// many(T1, T2, ...): list; singleton lifting allowed; multiple args imply union.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:50
public final class DslTypeMany extends ImmutableDslType {

    private final List<DslType> itemChoices;

    public DslTypeMany(MappedIterable<Expression> sourceAst, List<? extends DslType> itemChoices) {
        super(sourceAst);
        Objects.requireNonNull(itemChoices, "itemChoices must not be null");
        this.itemChoices = Collections.unmodifiableList(new ArrayList<>(itemChoices));
    }

    public List<DslType> getItemChoices() {
        return itemChoices;
    }
}
