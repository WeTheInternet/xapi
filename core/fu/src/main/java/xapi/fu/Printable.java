package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface Printable <Self extends Printable<Self>> extends Coercible {

  String NEW_LINE = "\n";

  Self append(final CharSequence s, final int start, final int end);

  Self append(final char[] str, final int offset, final int len);

  boolean isIndentNeeded();

  Self setIndentNeeded(boolean needed);

  String getIndent();

  Self setIndent(String indent);

  String toSource();

  static SimplePrintable newPrinter() {
    return toPrinter(new StringBuilder());
  }
  static SimplePrintable toPrinter(StringBuilder b) {
    return new SimplePrintable(b);
  }

  class SimplePrintable implements Printable<SimplePrintable> {

    private final StringBuilder target;
    private String indent;
    private boolean needsIndent;

    public SimplePrintable() {
      this(new StringBuilder());
    }

    public SimplePrintable(StringBuilder builder) {
      this.target = builder;
      indent = "";
    }

    @Override
    public SimplePrintable append(CharSequence s, int start, int end) {
      onAppend();
      target.append(s, start, end);
      return self();
    }

    @Override
    public SimplePrintable append(char[] str, int offset, int len) {
      onAppend();
      target.append(str, offset, len);
      return self();
    }

    @Override
    public boolean isIndentNeeded() {
      return needsIndent;
    }

    @Override
    public SimplePrintable setIndentNeeded(boolean needed) {
      needsIndent = needed;
      return self();
    }

    @Override
    public String getIndent() {
      return indent;
    }

    @Override
    public SimplePrintable setIndent(String indent) {
      this.indent = indent;
      return self();
    }

    @Override
    public String toSource() {
      return target.toString();
    }
  }

  default Self println() {
    onAppend();
    append(NEW_LINE);
    setIndentNeeded(true);
    return self();
  }

  default void printIndent() {
    if (isIndentNeeded()) {
      append(getIndent());
      setIndentNeeded(false);
    }
  }

  default void onAppend(){}

  default Self append(final CharSequence s) {
    append(s, 0, s.length());
    return self();
  }

  default Self print(final String str) {
    printIndent();
    append(str);
    return self();
  }

  default Self append(final boolean b) {
    return append(coerce(b));
  }

  default Self append(final char c) {
    return append(coerce(c));
  }

  default Self append(final int i) {
    return append(coerce(i));
  }

  default Self append(final long lng) {
    return append(coerce(lng));
  }

  default Self append(final float f) {
    return append(coerce(f));
  }

  default Self append(final double d) {
    return append(coerce(d));
  }

  default Self append(final Object s) {
    append(coerce(s));
    return self();
  }

  default Self append(final char[] str) {
    append(str, 0, str.length);
    return self();
  }

  default String perIndent() {
    return "  ";
  }

  default Self indent() {
    setIndent(getIndent() + perIndent());
    return self();
  }


  default Self indentln(final Object obj) {
    printIndent();
    onAppend();
    append(perIndent());
    append(coerce(obj));
    println();
    return self();
  }

  default Self indentln(final String str) {
    printIndent();
    onAppend();
    append(perIndent());
    append(str);
    println();
    return self();
  }

  default Self indentln(final CharSequence s) {
    printIndent();
    onAppend();
    append(perIndent());
    append(s);
    println();
    return self();
  }

  default Self indentln(final char[] str) {
    printIndent();
    onAppend();
    append(perIndent());
    append(str);
    println();
    return self();
  }

  default Self outdent() {
    String indent = getIndent();
    String per = perIndent();
    final int end = Math.max(0, indent.length() - per.length());
    if (end > 0) {
      setIndent(indent.substring(0, end));
    } else {
      setIndent("");
    }
    return self();
  }

  default Self println(final Object obj) {
    printIndent();
    onAppend();
    append(coerce(obj));
    println();
    return self();
  }

  default Self println(final String str) {
    printIndent();
    onAppend();
    append(str);
    println();
    return self();
  }

  default Self printlns(String str) {
    for (String line : str.split("\\r|(?:\\r?\\n)")) {
      println(line.trim());
    }
    return self();
  }


  default Self println(final CharSequence s) {
    printIndent();
    onAppend();
    append(s);
    println();
    return self();
  }

  default Self println(final char[] str) {
    printIndent();
    onAppend();
    append(str);
    println();
    return self();
  }

  default Self clearIndent() {
    setIndent("");
    return self();
  }

  default Self add(Object ... values) {
    for (Object value : values) {
      append(value);
    }
    return self();
  }

  default Self ln() {
    println();
    return self();
  }

  default Self self() {
    return (Self) this;
  }


}
