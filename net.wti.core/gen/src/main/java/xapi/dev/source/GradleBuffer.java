package xapi.dev.source;

import xapi.fu.Printable;
import xapi.string.X_String;

/**
 * GradleBuffer:
 * <p>
 * <p>
 * <p> A source-building buffer for gradle code.
 * <p>
 * <p> We compose a {@link MethodBuffer}, but only write to and print the method body,
 * <p> while providing convenience methods and generator-structures for gradle-related code.
 * <p>
 * <p> This is primarily used to generate buildscripts on the fly from a settings.gradle plugin.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 21/03/2021 @ 1:55 a.m.
 */
public class GradleBuffer implements Printable<GradleBuffer> {

    protected final MethodBuffer buffer;
    private boolean returnVoid;
    protected final GradleBuffer owner;

    public GradleBuffer() {
        final SourceBuilder<?> ctx = new SourceBuilder<>("class ignored{}");
        this.buffer = new MethodBuffer(ctx);
        this.buffer.getImports().setSkipImportRegex("org.gradle.api.*");
        this.buffer.setIndent("");
        this.owner = null;
    }

    public GradleBuffer(final GradleBuffer owner) {
        this.owner = owner;
        // share our import space, but not our body-write-space
        this.buffer = new MethodBuffer(owner == null ? new SourceBuilder<>("class ignored{}") : owner.buffer.getContext());
        if (owner != null) {
            this.buffer.setIndent(owner.buffer.getIndent() + Printable.INDENT);
        }
        this.buffer.getImports().setSkipImportRegex("org.gradle.api.*");
    }

    @Override
    public GradleBuffer append(final CharSequence s, final int start, final int end) {
        buffer.append(s, start, end);
        return this;
    }

    @Override
    public GradleBuffer append(final char[] str, final int offset, final int len) {
        buffer.append(str, offset, len);
        return this;
    }

    @Override
    public boolean isIndentNeeded() {
        return buffer.isIndentNeeded();
    }

    @Override
    public GradleBuffer setIndentNeeded(final boolean needed) {
        buffer.setIndentNeeded(needed);
        return this;
    }

    @Override
    public String getIndent() {
        return buffer.getIndent();
    }

    @Override
    public GradleBuffer setIndent(final String indent) {
        buffer.setIndent(indent);
        return this;
    }

    @Override
    public String toSource() {
        StringBuilder b = new StringBuilder();
        if (owner == null) {
            final String importSource = buffer.getImports().toSource();
            if (X_String.isNotEmptyTrimmed(importSource)) {
                b.append(importSource).append('\n').append(getIndent());
            }
        }
        b.append(buffer.toStringBodyOnly());
        if (isReturnVoid()) {
            b.append("\n").append(getIndent());
        }
        return b.toString();
    }

    public ClosureBuffer startClosure() {
        return startClosure(buffer);
    }

    public ClosureBuffer startClosure(String name) {
        return startClosure(buffer).setName(name);
    }

    protected ClosureBuffer startClosure(CharBuffer into) {
        final ClosureBuffer parent = this instanceof ClosureBuffer ? (ClosureBuffer) this : null;
        ClosureBuffer kid = new ClosureBuffer(parent, this);
        // "physically" attach our closure.
        final boolean finishLine = isIndentNeeded();
        if (finishLine) {
            println();
        }
        into.addToEnd(new CharBuffer() {

            @Override
            public String toSource() {
                return kid.toSource();
            }
        });
        return kid;
    }

    public String addImport(Class<?> cls) {
        return buffer.addImport(cls);
    }

    public String addImport(String cls) {
        return buffer.addImport(cls);
    }

    public String addImportStatic(Class<?> cls, String name) {
        return buffer.addImportStatic(cls, name);
    }

    public String addImportStatic(String cls, String name) {
        return buffer.addImportStatic(cls, name);
    }

    public String addImportStatic(String cls) {
        return buffer.addImportStatic(cls);
    }

    /**
     * @return true if this buffer should print a void return at the end of the closure.
     * <p>
     * <p> This is useful when you want to explicitly target the Action interface,
     * <p> as gradle commonly overloads Closure and Action, which can lead to ambiguity.
     */
    public boolean isReturnVoid() {
        return returnVoid;
    }

    public void setReturnVoid(final boolean returnVoid) {
        this.returnVoid = returnVoid;
    }

    public LocalVariable addVariable(final Class<?> cls, final String name) {
        return this.buffer.newVariable(cls, name);
    }

    public LocalVariable addVariable(final Class<?> cls, final String name, boolean reuseExisting) {
        return this.buffer.newVariable(cls, name, reuseExisting);
    }

    public LocalVariable addVariable(final String cls, final String name) {
        return this.buffer.newVariable(cls, name);
    }

    public LocalVariable addVariable(final String cls, final String name, boolean reuseExisting) {
        return this.buffer.newVariable(cls, name, reuseExisting);
    }

    @Override
    public Printable<?> printBefore(String before) {
        return buffer.printBefore(before);
    }

    @Override
    public Printable<?> printAfter(String before) {
        return buffer.printAfter(before);
    }

    @Override
    public GradleBuffer println() {
        try {
            return Printable.super.println();
        } finally {
            setIndentNeeded(true);
        }
    }

    @Override
    public GradleBuffer println(final String str) {
        return Printable.super.println(str);
    }
}
