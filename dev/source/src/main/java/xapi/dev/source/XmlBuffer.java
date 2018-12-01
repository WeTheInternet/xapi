package xapi.dev.source;

import xapi.fu.In1Out1;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

import static xapi.fu.In1Out1.identity;

public class XmlBuffer extends PrintBuffer {

  public static final String QUOTE = "\"", QUOTE_ENTITY = "&quot;";
  private static final String COMMENT_NAME = "!--";

  private String tagName;
  private PrintBuffer attributes;

  private final PrintBuffer comment;

  private final PrintBuffer before;

  protected static class AttrNode {
    protected final StringBuilder raw;
    protected PrintBuffer live;

    public AttrNode(StringBuilder raw) {
      this.raw = raw;
    }
  }

  private MapLike<String, AttrNode> attributeMap;
  protected boolean printNewline = false;
  private boolean attributeNewline = false;
  private boolean abbr = false;
  private In1Out1<String, String> escaper;
  protected boolean trimWhitespace;
  private String doctype;

  @SuppressWarnings("unchecked")
  public XmlBuffer() {
    comment = new PrintBuffer();
    before = new PrintBuffer();
    init();
  }

  public XmlBuffer(final String tagName) {
    this();
    setTagName(tagName);
  }

  XmlBuffer(StringBuilder suffix) {
    super(suffix);
    comment = new PrintBuffer();
    before = new PrintBuffer();
    init();
  }

  protected void init() {
    indent = INDENT;
    escaper = identity();
  }

  public XmlBuffer setTagName(final String name) {
    if (tagName != null) {
      indent();
    }
    tagName = name;
    return this;
  }

  @Override
  public PrintBuffer setIndentNeeded(boolean needed) {
    comment.setIndentNeeded(needed);
    before.setIndentNeeded(needed);
    return super.setIndentNeeded(needed);
  }

  public XmlBuffer setAttributeIfNotNull(final String name, final String value) {
    if (value == null) {
      return this;
    }
    return setAttribute(name, value, true);
  }

  public XmlBuffer setAttribute(final String name, final String value) {
    return setAttribute(name, value, true);
  }
  public XmlBuffer setAttribute(final String name, final String value, boolean encodeValue) {
    ensureAttributes();
    // TODO consider a null value to mean "clear this attribute"?
    // This is a widely used class, so these semantics should not be changed
    // without tests, or in a commit without any unrelated changes.
    final String val = encodeValue ? escapeAttribute(value) : value == null ? " " : " = " + (value.isEmpty() ? "" : value + " ");
    setRawAttribute(name, val);
    return this;
  }

  public PrintBuffer attribute(final String name) {
    ensureAttributes();
    if (attributeMap.has(name)) {
      return attributeMap.get(name).live;
    }
    setAttribute(name, "", false);
    return attributeMap.get(name).live;
  }

  public String escapeAttributeValue(String value) {
    if (value == null) {
      return "\"\"";
    }
    return "\"" + value.replaceAll(QUOTE, QUOTE_ENTITY) + "\" ";
  }

  protected String escapeAttribute(final String value) {
    if (value == null) {
      return " ";
    } else {
      if (value.startsWith("//")) {
        return "=" + escape(value.substring(2));
      } else {
        return "=" + escapeAttributeValue(value);
      }
    }
  }

  protected void setRawAttribute(final String name, String val) {
    AttrNode attr = attributeMap.get(name);
    if (attr == null) {
      if (isAttributeNewline()) {
        attributes.println();
        val = val.trim();
      }
      attributes.print(name);
      attr = new AttrNode(new StringBuilder(val));
      attributeMap.put(name, attr);
      attr.live = new PrintBuffer(attr.raw);
      attributes.addToEnd(attr.live);
    } else {
      attr.raw.setLength(0);
      attr.raw.append(val);
    }
  }

  protected void ensureAttributes() {
    if (attributes == null) {
      attributes = new PrintBuffer();
      attributeMap = X_Jdk.mapOrderedInsertion();
    }
  }

  public XmlBuffer makeTag(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setNewLine(printNewline);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.abbr = abbr;
    buffer.indent = indent + INDENT;
    buffer.attributeNewline = attributeNewline;
    addToEnd(buffer);
    return buffer;
  }

  public XmlBuffer makeTagNoIndent(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.abbr = abbr;
    buffer.indent = indent + INDENT;
    buffer.setNewLine(false);
    buffer.attributeNewline = attributeNewline;
    addToEnd(buffer);
    buffer.setIndentNeeded(false);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginning(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.abbr = abbr;
    buffer.setNewLine(printNewline);
    buffer.indent = indent + INDENT;
    buffer.attributeNewline = attributeNewline;
    addToBeginning(buffer);
    return buffer;
  }

  public XmlBuffer makeTagAtBeginningNoIndent(final String name) {
    final XmlBuffer buffer = new XmlBuffer(name);
    buffer.setNewLine(false);
    buffer.abbr = abbr;
    buffer.setTrimWhitespace(trimWhitespace);
    buffer.indent = indent + INDENT;
    buffer.attributeNewline = attributeNewline;
    addToBeginning(buffer);
    buffer.setIndentNeeded(false);
    return buffer;
  }

  public String toSource() {
    String dt = doctype == null ? ""
              : doctype.startsWith("<!") ? (doctype.endsWith("\n") ? doctype : doctype+"\n")
              : "<!DOCTYPE " + doctype + ">\n";
    if (tagName == null) {
      assert attributes == null || attributes.isEmpty() : "Cannot add attributes to an XmlBuffer with no tag name: "
          + "\nAttributes: " + attributes + "\nBody: " + super.toSource();
      return dt + super.toSource();
    }
    String indent = trimWhitespace ? "" : this.indent;
    final String origIndent = indent.replaceFirst(INDENT, "");
    final StringBuilder b = new StringBuilder(dt);
    b.append(origIndent);
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
      String s = attributes.toSource();
      if (!s.isEmpty()) {
        if (!Character.isWhitespace(s.charAt(0))) {
          b.append(" ");
        }
      }
      b.append(s);
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
    return escaper.io(text);
  }

  public XmlBuffer setEscaper(final In1Out1<String, String> escaper) {
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
  public XmlBuffer print(final CharSequence str) {
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

  public String getTagName() {
    return tagName;
  }

  public boolean hasComment() {
    return comment.isEmpty();
  }

  /**
   * final so implementors must override addComment.
   *
   * We pay for the ugly unsafe generic to make it final,
   * so perhaps a nice @DoNotOverride annotation is in order
   * or @MustCallSuper (which would make the infinite recursion obvious),
   * or @NeverCallFrom() (which would check all subtype's method bodies)
   */
  @SuppressWarnings("unchecked")
  public final <X extends XmlBuffer> X setComment(String comment) {
    this.comment.clear();
    addComment(comment);
    return (X) this;
  }

  public XmlBuffer addComment(String comment) {
    this.comment.append(comment);
    return this;
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
    if (!attributeMap.has("id")) {
      setId("x-"+hashCode());
    }
    final AttrNode id = attributeMap.get("id");
    return id.raw.substring(2, id.raw.length()-2);
  }

  public boolean hasAttribute(final String name) {
    ensureAttributes();
    return attributeMap.has(name);
  }

  public String getAttribute(final String name) {
    if (hasAttribute(name)) {
      final AttrNode attr = attributeMap.get(name);
      if (isRemoveQuotes(attr.raw)) {
        return attr.raw.substring(2, attr.raw.length()-2);
      }
      return attr.raw.substring(1, attr.raw.length());
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

  public String getDoctype() {
    return doctype;
  }

  public XmlBuffer setDoctype(String doctype) {
    this.doctype = doctype;
    return this;
  }

  public boolean isAttributeNewline() {
    return attributeNewline;
  }

  public XmlBuffer setAttributeNewline(boolean attributeNewline) {
    this.attributeNewline = attributeNewline;
    return this;
  }

  @Override
  public XmlBuffer makeChild() {
    XmlBuffer child = (XmlBuffer) super.makeChild();
    child.attributeNewline = attributeNewline;
    return child;
  }
}
