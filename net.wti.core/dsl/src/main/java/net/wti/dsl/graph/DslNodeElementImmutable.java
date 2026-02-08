package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslNodeElementImmutable:
///
/// Immutable implementation of {@link DslNodeElement}.
///
/// This class is intended to be produced by analysis/build steps (or by a builder)
/// and is safe to share freely.
///
/// Invariants:
///  - tagName is non-empty,
///  - attribute and child lists are non-null and contain no null entries,
///  - ordering is preserved as provided.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslNodeElementImmutable extends ImmutableDslNode implements DslNodeElement {

    private final String tagName;
    private final List<AttrEntry> attributes;
    private final List<DslNodeElement> children;

    public DslNodeElementImmutable(
            final MappedIterable<Expression> sourceAst,
            final String tagName,
            final List<? extends AttrEntry> attributes,
            final List<? extends DslNodeElement> children
    ) {
        super(sourceAst);
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName must not be null/empty");
        }
        this.tagName = tagName;

        Objects.requireNonNull(attributes, "attributes must not be null");
        final ArrayList<AttrEntry> attrCopy = new ArrayList<>(attributes.size());
        for (final AttrEntry e : attributes) {
            attrCopy.add(Objects.requireNonNull(e, "attribute entry must not be null"));
        }
        this.attributes = Collections.unmodifiableList(attrCopy);

        Objects.requireNonNull(children, "children must not be null");
        final ArrayList<DslNodeElement> childCopy = new ArrayList<>(children.size());
        for (final DslNodeElement c : children) {
            childCopy.add(Objects.requireNonNull(c, "child must not be null"));
        }
        this.children = Collections.unmodifiableList(childCopy);
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
}
