package xapi.dev.source;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.util.api.ConvertsValue;

public class XmlBuffer extends PrintBuffer {

  public static final String QUOTE = "\"", QUOTE_ENTITY = "&quot;";

  private String tagName;
  private PrintBuffer attributes, comment, before;
  private StringTo<StringBuilder> attributeMap;
  private boolean printNewline = false;
  private boolean abbr = false;
  private ConvertsValue<String, String> escaper;

  @SuppressWarnings("unchecked")
  public XmlBuffer() {
    comment = new PrintBuffer();
    before = new PrintBuffer();
    indent = INDENT;
    escaper = ConvertsValue.PASS_THRU;
  }

  public XmlBuffer(String tagName) {
    this();
    setTagName(tagName);
  }

  public XmlBuffer setTagName(String name) {
    if (tagName != null) {
      indent();
    }
    tagName = name;
    return this;
  }

  public XmlBuffer setAttribute(String name, String value) {
    ensureAttributes();
    String val = escapeAttribute(value);
    setRawAttribute(name, val);
    return this;
  }

  protected String escapeAttribute(String value) {
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

  protected void setRawAttribute(String name, String val) {
    StringBuilder attr = attributeMap.get(name);
    if (attr == null) {
      attributes.print(name);
      attr = new StringBuilder(val);
      attributeMap.put(name, attr);
      PrintBuffer attrBuf = new PrintBuffer(attr);
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

  public XmlBuffer makeTag(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.indent = indent + INDENT;
    addToEnd(buffer);
    return buffer;
  }

  public XmlBuffer makeTagNoIndent(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.indent = indent + INDENT;
    buffer.setNewLine(false);
    addToEnd(buffer);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginning(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginningNoIndent(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.setNewLine(false);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  @Override
  public String toString() {
    if (tagName == null) {
      assert attributes == null || attributes.isEmpty() : "Cannot add attributes to an XmlBuffer with no tag name: "
          + "\nAttributes: " + attributes + "\nBody: " + super.toString();
      return super.toString();
    }
    String origIndent = indent.replaceFirst(INDENT, "");
    StringBuilder b = new StringBuilder(origIndent);

    String text;
    text = this.before.toString();
    if (text.length() > 0) {
      b.append(escape(text));
    }
    text = this.comment.toString();
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
    if (attributes != null) {
      b.append(" ").append(attributes);
    }
    String body = super.toString();
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

  public String escape(String text) {
    return escaper.convert(text);
  }

  public XmlBuffer setEscaper(ConvertsValue<String, String> escaper) {
    this.escaper = escaper;
    return this;
  }

  private StringBuilder newline(StringBuilder append) {
    if (printNewline) {
      append.append("\n");
    }
    return append;
  }

  public XmlBuffer setNewLine(boolean useNewLine) {
    printNewline = useNewLine;
    return this;
  }

  protected boolean shouldShortenEmptyTag(String tag) {
    return !"script".equals(tag);
  }

  @Override
  public XmlBuffer append(Object obj) {
    super.append(obj);
    return this;
  }

  @Override
  public XmlBuffer print(String str) {
    super.print(str);
    return this;
  }

  @Override
  public XmlBuffer clearIndent() {
    indent = "";
    return this;
  }

  @Override
  public XmlBuffer append(String str) {
    super.append(str);
    return this;
  }

  @Override
  public XmlBuffer append(CharSequence s) {
    super.append(s);
    return this;
  }

  @Override
  public XmlBuffer append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return this;
  }

  @Override
  public XmlBuffer append(char[] str) {
    super.append(str);
    return this;
  }

  @Override
  public XmlBuffer append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return this;
  }

  @Override
  public XmlBuffer append(boolean b) {
    super.append(b);
    return this;
  }

  @Override
  public XmlBuffer append(char c) {
    super.append(c);
    return this;
  }

  @Override
  public XmlBuffer append(int i) {
    super.append(i);
    return this;
  }

  @Override
  public XmlBuffer append(long lng) {
    super.append(lng);
    return this;
  }

  @Override
  public XmlBuffer append(float f) {
    super.append(f);
    return this;
  }

  @Override
  public XmlBuffer append(double d) {
    super.append(d);
    return this;
  }

  @Override
  public XmlBuffer indent() {
    super.indent();
    return this;
  }

  @Override
  public XmlBuffer indentln(Object obj) {
    super.indentln(obj);
    return this;
  }

  @Override
  public XmlBuffer indentln(String str) {
    super.indentln(str);
    return this;
  }

  @Override
  public XmlBuffer indentln(CharSequence s) {
    super.indentln(s);
    return this;
  }

  @Override
  public XmlBuffer indentln(char[] str) {
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
  public XmlBuffer println(Object obj) {
    super.println(obj);
    return this;
  }

  @Override
  public XmlBuffer println(String str) {
    super.println(str);
    return this;
  }

  @Override
  public XmlBuffer println(CharSequence s) {
    super.println(s);
    return this;
  }

  @Override
  public XmlBuffer println(char[] str) {
    super.println(str);
    return this;
  }

  @Override
  public PrintBuffer printBefore(String prefix) {
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

  public XmlBuffer setId(String id) {
    setAttribute("id", id);
    return this;
  }

  public String getId() {
    if (!attributeMap.containsKey("id")) {
      setId("x-"+hashCode());
    }
    StringBuilder id = attributeMap.get("id");
    return id.substring(2, id.length()-2);
  }

  public boolean hasAttribute(String name) {
    ensureAttributes();
    return attributeMap.containsKey(name);
  }

  public String getAttribute(String name) {
    if (hasAttribute(name)) {
      StringBuilder attr = attributeMap.get(name);
      if (isRemoveQuotes(attr)) {
        return attr.substring(2, attr.length()-2);
      }
      return attr.substring(1, attr.length());
    }
    return null;
  }

  protected boolean isRemoveQuotes(StringBuilder attr) {
    return attr.charAt(1) == '"';
  }

  public XmlBuffer allowAbbreviation(boolean abbr) {
    this.abbr = abbr;
    return this;
  }

}
