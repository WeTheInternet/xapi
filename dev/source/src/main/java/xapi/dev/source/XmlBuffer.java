package xapi.dev.source;

public class XmlBuffer extends PrintBuffer {

  private String tagName;
  private PrintBuffer attributes, comment;

  public XmlBuffer() {
    comment = new PrintBuffer();
    indent = INDENT;
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
    attributes.print(name);
    if (value != null) {
      attributes
        .print("=\"")
        .print(value.replaceAll("\"", "&quot;"))
        .print("\"");
    }
    attributes.print(" ");
    return this;
  }
  
  private void ensureAttributes() {
    if (attributes == null) {
      attributes = new PrintBuffer();
    }
  }
  public XmlBuffer makeTag(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.indent = indent + INDENT;
    addToEnd(buffer);
    return buffer;
  }
  public XmlBuffer makeTagAtBeginning(String name) {
    XmlBuffer buffer = new XmlBuffer(name);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }
  
  @Override
  public String toString() {
    if (tagName == null) {
      assert attributes == null : "Cannot add attributes to an XmlBuffer with no tag name: "
          + "\nAttributes: "+ attributes
          + "\nBody: "+super.toString();
      return super.toString();
    }
    String origIndent = indent.replaceFirst(INDENT, "");
    StringBuilder b = new StringBuilder(origIndent);
    String comment = this.comment.toString();
    if (comment.length() > 0) {
      if (!comment.startsWith("<!--")) {
        b.append("<!--\n");
      }
      b.append(indent);
      b.append(comment);
      if (!comment.endsWith("-->")) {
        b.append("\n-->");
      }
      b.append(origIndent);
    }
    b
      .append("<")
      .append(tagName);
    if (attributes != null) {
      b.append(" ").append(attributes);
    }
    String body = super.toString();
    if (body.length() == 0) {
      b.append("/>\n");
    } else {
      b.append(">\n")
       .append(body)
       .append(origIndent)
       .append("</")
       .append(tagName)
       .append(">\n");
    }
    return b.toString();
  }
  

  public XmlBuffer append(Object obj) {
    super.append(obj);
    return this;
  }

  public XmlBuffer print(String str) {
    super.print(str);
    return this;
  }
  public XmlBuffer append(String str) {
    super.append(str);
    return this;
  }

  public XmlBuffer append(CharSequence s) {
    super.append(s);
    return this;
  }

  public XmlBuffer append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return this;
  }

  public XmlBuffer append(char[] str) {
    super.append(str);
    return this;
  }

  public XmlBuffer append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return this;
  }

  public XmlBuffer append(boolean b) {
    super.append(b);
    return this;
  }

  public XmlBuffer append(char c) {
    super.append(c);
    return this;
  }

  public XmlBuffer append(int i) {
    super.append(i);
    return this;
  }

  public XmlBuffer append(long lng) {
    super.append(lng);
    return this;
  }

  public XmlBuffer append(float f) {
    super.append(f);
    return this;
  }

  public XmlBuffer append(double d) {
    super.append(d);
    return this;
  }

  public XmlBuffer indent() {
    super.indent();
    return this;
  }

  public XmlBuffer indentln(Object obj) {
    super.indentln(obj);
    return this;
  }

  public XmlBuffer indentln(String str) {
    super.indentln(str);
    return this;
  }

  public XmlBuffer indentln(CharSequence s) {
    super.indentln(s);
    return this;
  }

  public XmlBuffer indentln(char[] str) {
    super.indentln(str);
    return this;
  }

  public XmlBuffer outdent() {
    super.outdent();
    return this;
  }

  public XmlBuffer println() {
    super.println();
    return this;
  }

  public XmlBuffer println(Object obj) {
    super.println(obj);
    return this;
  }

  public XmlBuffer println(String str) {
    super.println(str);
    return this;
  }

  public XmlBuffer println(CharSequence s) {
    super.println(s);
    return this;
  }

  public XmlBuffer println(char[] str) {
    super.println(str);
    return this;
  }
  
}
