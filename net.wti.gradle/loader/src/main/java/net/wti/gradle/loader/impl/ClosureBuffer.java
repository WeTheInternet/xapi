package net.wti.gradle.loader.impl;

import xapi.dev.source.LocalVariable;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.util.X_String;

/**
 * ClosureBuffer:
 * <p>
 * <p>
 * <p> A {@link GradleBuffer} which provides { closure, like -> structuresAvailable(); }
 * <p>
 * <p> This class always inserts { -> }, and you can alter the closure parameters any time before printing.
 * <p>
 * <p> We simply override {@link #toSource()}, to add the decorations.
 * <p>
 * <p>
 * <p> When you create a closure using a {@link GradleBuffer}, that buffer will "own" this one,
 * <p>and if a closure is created within another closure, it remembers the parent closure.
 * <p>
 * <p>This allows you to inspect, access or even mutate parent and sibling scopes as you please.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 21/03/2021 @ 2:09 a.m..
 */
public class ClosureBuffer extends GradleBuffer {

    private final ClosureBuffer parent;
    private final MapLike<String, LocalVariable> variables;
    private String name;
    private boolean alwaysPrintArrow;

    public ClosureBuffer() {
        this.parent = null;
        variables = X_Jdk.mapOrderedInsertion();
    }

    public ClosureBuffer(ClosureBuffer parent, GradleBuffer owner) {
        super(owner);
        this.parent = parent;
        variables = X_Jdk.mapOrderedInsertion();
    }

    public ClosureBuffer getParent() {
        return parent;
    }

    /**
     * The "owner buffer" where this closure will be printing to.
     * <p>
     * <p> If parent != null, it implies owner == parent, but this is not enforced;
     * <p> you can add your own assertions.
     *
     * @return The {@link GradleBuffer} which "owns and will be printing" this closure.
     */
    public GradleBuffer getOwner() {
        return owner;
    }

    @Override
    public String toSource() {
        final StringBuilder b = new StringBuilder();
        if (X_String.isNotEmptyTrimmed(name)) {
            b.append(name).append(" ");
        }
        b.append("{");
        final String body = super.toSource();

        final String[] lines = body.split("[\\n\\r]+");
        boolean multiline = lines.length > 1 || isIndentNeeded();

        printParameters(b, multiline);

        if (multiline) {
            String prefix = "";
            for (String line : lines) {
                b.append(prefix).append(line).append("\n");
                prefix = INDENT;
            }
        } else {
            b.append(body);
        }

        b.append("}");
        if (multiline) {
            b.append("\n");
        }
        return b.toString();
    }

    public LocalVariable addParameter(Class<?> cls, String name) {
        return addParameter(addImport(cls), name);
    }

    public LocalVariable addParameter(String cls, String name) {
        return variables.computeIfAbsent(name, ()->
            new LocalVariable(buffer, cls, name)
        );
    }

    protected void printParameters(final StringBuilder into, final boolean multiline) {
        String prefix = "";
        if (multiline && variables.size() > 2) {
            for (LocalVariable value : variables.mappedValues()) {
                into.append(prefix).append(value.toSource());
                prefix = ", ";
                into.append("\n").append(getIndent());
            }
        } else {
            for (LocalVariable value : variables.mappedValues()) {
                into.append(prefix).append(value.toSource());
                prefix = ", ";
            }
        }
        if (variables.isNotEmpty() || isAlwaysPrintArrow()) {
            into.append(" ->");
        }
        if (multiline) {
            into.append("\n").append(getIndent());
        }
    }

    public ClosureBuffer setName(final String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isAlwaysPrintArrow() {
        return alwaysPrintArrow;
    }

    public void setAlwaysPrintArrow(final boolean alwaysPrintArrow) {
        this.alwaysPrintArrow = alwaysPrintArrow;
    }
}
