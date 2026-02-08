package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

///
/// DslNodeElementBuilder:
///
/// Mutable builder variant of {@link DslNodeElement}.
///
/// This builder preserves:
///  - attribute ordering (instruction-list semantics),
///  - child ordering,
///  - dynamic provenance accumulation via {@link AbstractDslNodeBuilder#addSource(Expression)}.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslNodeElementBuilder extends AbstractDslNodeBuilder<DslNodeElementImmutable> implements DslNodeElement {

    private String tagName;
    private final ArrayList<AttrEntry> attributes;
    private final ArrayList<DslNodeElement> children;

    public DslNodeElementBuilder() {
        super();
        this.attributes = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public DslNodeElementBuilder setTagName(final String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName must not be null/empty");
        }
        this.tagName = tagName;
        return this;
    }

    public DslNodeElementBuilder addAttribute(final AttrEntry entry) {
        attributes.add(Objects.requireNonNull(entry, "entry must not be null"));
        return this;
    }

    public DslNodeElementBuilder addChild(final DslNodeElement child) {
        children.add(Objects.requireNonNull(child, "child must not be null"));
        return this;
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public List<AttrEntry> getAttributes() {
        return attributes;
    }

    @Override
    public List<DslNodeElement> getChildren() {
        return children;
    }

    @Override
    public DslNodeElementImmutable buildImmutable() {
        return new DslNodeElementImmutable(
                getSourceAst(),
                Objects.requireNonNull(tagName, "tagName must be set before buildImmutable()"),
                attributes,
                children
        );
    }
}
