package xapi.dev.source;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.util.api.ConvertsValue;

public class XmlBuffer extends PrintBuffer {

  public static final String QUOTE = "\"", QUOTE_ENTITY = "&quot;";

  private String tagName;
  private PrintBuffer attributes;

  private final PrintBuffer comment;

  private final PrintBuffer before;
  private StringTo<StringBuilder> attributeMap;
  protected boolean printNewline = false;
  private boolean abbr = false;
  private ConvertsValue<String, String> escaper;
  protected boolean trimWhitespace;

  @SuppressWarnings("unchecked")
  public XmlBuffer() {
    comment = new PrintBuffer();
    before = new PrintBuffer();
    indent = INDENT;
    escaper = ConvertsValue.PASS_THRU;
  }

  public XmlBuffer(final String tagName) {
    this();
    setTagName(tagName);
  }

  public XmlBuffer setTagName(final String name) {
    if (tagName != null) {
      indent();
    }
    tagName = name;
    return this;
  }

  public XmlBuffer setAttribute(final String name, final String value) {
    ensureAttributes();
    final String val = escapeAttribute(value);
    setRawAttribute(name, val);
    return this;
  }

  protected String escapeAttribute(final String value) {
    if (value == null) {
      return " ";
    } else {
      if (value.startsWith("//")) {
        return "=" + escape(value.substring(2));
      } else {
        return "=\"" + value.replaceAll(QUOTE, QUOTE_ENTITY) + "\" ";
      }
    }
  }

  protected void setRawAttribute(final String name, final String val) {
    StringBuilder attr = attributeMap.get(name);
    if (attr == null) {
      attributes.print(name);
      attr = new StringBuilder(val);
      attributeMap.put(name, attr);
      final PrintBuffer attrBuf = new PrintBuffer(attr);
      attributes.addToEnd(attrBuf);
    } else {
      attr.setLength(0);
      attr.append(val);
    }
  }

  protected void ensureAttributes() {
    if (attributes == null) {
      attributes = new PrintBuffer();
      attributeMap = X_Collect.newStringMapInsertionOrdered(StringBuilder.class);
    }
  }

  public XmlBuffer makeTag(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setTrimWhitespace(true);
    buffer.indent = indent + INDENT;
    addToEnd(buffer);
    return buffer;
  }

  public XmlBuffer makeTagNoIndent(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.indent = indent + INDENT;
    buffer.setNewLine(false);
    addToEnd(buffer);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginning(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginningNoIndent(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setNewLine(false);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  public String toSource() {
    if (tagName == null) {
      assert attributes == null || attributes.isEmpty() : "Cannot add attributes to an XmlBuffer with no tag name: "
          + "\nAttributes: " + attributes + "\nBody: " + super.toSource();
      return super.toSource();
    }
    String indent = trimWhitespace ? "" : this.indent;
    final String origIndent = indent.replaceFirst(INDENT, "");
    final StringBuilder b = new StringBuilder(origIndent);

    String text;
    text = this.before.toSource();
    if (text.length() > 0) {
      b.append(escape(text));
    }
    text = this.comment.toSource();
    if (text.length() > 0) {
      if (!text.startsWith("<!--")) {
        b.append("<!--\n");
      }
      b.append(indent);
      b.append(escape(text));
      if (!text.endsWith("-->")) {
        b.append("\n-->");
      }
      b.append(origIndent);
    }

    b.append("<");
    String tag;
    if (tagName.startsWith("//")) {
      tag = escape(tagName.substring(2));
    } else {
      tag = tagName;
    }
    b.append(tag);
    if (attributes != null && !attributes.isEmpty()) {
      b.append(" ").append(attributes);
    }
    final String body = super.toSource();
    if (abbr && body.length() == 0) {
      if (shouldShortenEmptyTag(tagName)) {
        newline(b.append("/>"));
      } else {
        newline(b.append("> </").append(tag).append(">"));
      }
    } else {
      newline(newline(b.append(">")).append(escape(body)).append(printNewline ? origIndent : "").append("</")
                  .append(tag).append(">"));
    }
    return b.toString();
  }

  public String escape(final String text) {
    return escaper.convert(text);
  }

  public XmlBuffer setEscaper(final ConvertsValue<String, String> escaper) {
    this.escaper = escaper;
    return this;
  }

  private StringBuilder newline(final StringBuilder append) {
    if (printNewline && !trimWhitespace) {
      append.append("\n");
    }
    return append;
  }

  public XmlBuffer setNewLine(final boolean useNewLine) {
    printNewline = useNewLine;
    return this;
  }

  protected boolean shouldShortenEmptyTag(final String tag) {
    return !"script".equals(tag);
  }

  @Override
  public XmlBuffer append(final Object obj) {
    super.append(obj);
    return this;
  }

  @Override
  public XmlBuffer print(final String str) {
    super.print(str);
    return this;
  }

  @Override
  public XmlBuffer clearIndent() {
    indent = "";
    return this;
  }

  @Override
  public XmlBuffer append(final String str) {
    super.append(str);
    return this;
  }

  @Override
  public XmlBuffer append(final CharSequence s) {
    super.append(s);
    return this;
  }

  @Override
  public XmlBuffer append(final CharSequence s, final int start, final int end) {
    super.append(s, start, end);
    return this;
  }

  @Override
  public XmlBuffer append(final char[] str) {
    super.append(str);
    return this;
  }

  @Override
  public XmlBuffer append(final char[] str, final int offset, final int len) {
    super.append(str, offset, len);
    return this;
  }

  @Override
  public XmlBuffer append(final boolean b) {
    super.append(b);
    return this;
  }

  @Override
  public XmlBuffer append(final char c) {
    super.append(c);
    return this;
  }

  @Override
  public XmlBuffer append(final int i) {
    super.append(i);
    return this;
  }

  @Override
  public XmlBuffer append(final long lng) {
    super.append(lng);
    return this;
  }

  @Override
  public XmlBuffer append(final float f) {
    super.append(f);
    return this;
  }

  @Override
  public XmlBuffer append(final double d) {
    super.append(d);
    return this;
  }

  @Override
  public XmlBuffer indent() {
    super.indent();
    return this;
  }

  @Override
  public XmlBuffer indentln(final Object obj) {
    super.indentln(obj);
    return this;
  }

  @Override
  public XmlBuffer indentln(final String str) {
    super.indentln(str);
    return this;
  }

  @Override
  public XmlBuffer indentln(final CharSequence s) {
    super.indentln(s);
    return this;
  }

  @Override
  public XmlBuffer indentln(final char[] str) {
    super.indentln(str);
    return this;
  }

  @Override
  public XmlBuffer outdent() {
    super.outdent();
    return this;
  }

  @Override
  public XmlBuffer println() {
    super.println();
    return this;
  }

  @Override
  public XmlBuffer println(final Object obj) {
    super.println(obj);
    return this;
  }

  @Override
  public XmlBuffer println(final String str) {
    super.println(str);
    return this;
  }

  @Override
  public XmlBuffer println(final CharSequence s) {
    super.println(s);
    return this;
  }

  @Override
  public XmlBuffer println(final char[] str) {
    super.println(str);
    return this;
  }

  @Override
  public PrintBuffer printBefore(final String prefix) {
    return before.printBefore(prefix);
  }

  public boolean isNoTagName() {
    return tagName == null;
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty() && isNoTagName() && comment.isEmpty()
        && before.isEmpty();
  }

  public XmlBuffer setId(final String id) {
    setAttribute("id", id);
    return this;
  }

  public String getId() {
    if (!attributeMap.containsKey("id")) {
      setId("x-"+hashCode());
    }
    final StringBuilder id = attributeMap.get("id");
    return id.substring(2, id.length()-2);
  }

  public boolean hasAttribute(final String name) {
    ensureAttributes();
    return attributeMap.containsKey(name);
  }

  public String getAttribute(final String name) {
    if (hasAttribute(name)) {
      final StringBuilder attr = attributeMap.get(name);
      if (isRemoveQuotes(attr)) {
        return attr.substring(2, attr.length()-2);
      }
      return attr.substring(1, attr.length());
    }
    return null;
  }

  public XmlBuffer add(Object ... values) {
    super.add(values);
    return this;
  }

  @Override
  public XmlBuffer ln() {
    super.ln();
    return this;
  }

  protected boolean isRemoveQuotes(final StringBuilder attr) {
    return attr.charAt(1) == '"';
  }

  public XmlBuffer allowAbbreviation(final boolean abbr) {
    this.abbr = abbr;
    return this;
  }

  public XmlBuffer setTrimWhitespace(boolean trimWhitespace) {
    this.trimWhitespace = trimWhitespace;
    return this;
  }
}
