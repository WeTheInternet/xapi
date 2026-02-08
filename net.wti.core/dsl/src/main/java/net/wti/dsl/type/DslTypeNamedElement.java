package net.wti.dsl.type;


import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslTypeNamedElement:
///
/// namedElement("typeName"): self-keying element whose tag name is the instance name.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:52
public final class DslTypeNamedElement extends ImmutableDslType {

    private final String elementTypeName;

    public DslTypeNamedElement(MappedIterable<Expression> sourceAst, String elementTypeName) {
        super(sourceAst);
        this.elementTypeName = Objects.requireNonNull(elementTypeName, "elementTypeName must not be null");
    }

    public String getElementTypeName() {
        return elementTypeName;
    }
}
