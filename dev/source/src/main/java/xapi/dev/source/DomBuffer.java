package xapi.dev.source;

public class DomBuffer extends XmlBuffer {

  public DomBuffer() {
  }
  
  public DomBuffer(String name) {
    super(name);
  }
  
  public DomBuffer setClassName(String clsName) {
    setAttribute("class", clsName);
    return this;
  }

  public DomBuffer addClassName(String clsName) {
    if (hasAttribute("class")) {
      setAttribute("class", getAttribute("class")+" "+clsName);
    } else {
      setAttribute("class", clsName);
    }
    return this;
  }
  
  public DomBuffer setAlt(String alt) {
    setAttribute("alt", alt);
    return this;
  }
  
  public DomBuffer setHref(String href) {
    setAttribute("href", href);
    return this;
  }
  
  public DomBuffer setSrc(String src) {
    setAttribute("src", src);
    return this;
  }
  
  public DomBuffer setName(String name) {
    setAttribute("name", name);
    return this;
  }
  
  public DomBuffer setType(String type) {
    setAttribute("type", type);
    return this;
  }

  public DomBuffer setValue(String value) {
    setAttribute("value", value);
    return this;
  }
  
  public DomBuffer setAction(String action) {
    setAttribute("action", action);
    return this;
  }
  
  public DomBuffer setTarget(String target) {
    setAttribute("target", target);
    return this;
  }
  
  public DomBuffer setMethod(String method) {
    setAttribute("method", method);
    return this;
  }
  
  public DomBuffer setEnctype(String enctype) {
    setAttribute("enctype", enctype);
    return this;
  }
  
  public DomBuffer setRel(String rel) {
    setAttribute("rel", rel);
    return this;
  }

  public DomBuffer setTagName(String name) {
    super.setTagName(name);
    return this;
  }

  public DomBuffer setAttribute(String name, String value) {
    super.setAttribute(name, value);
    return this;
  }

  public DomBuffer makeHiddenIframe() {
    DomBuffer iframe = makeTag("iframe")
      .setClassName("hidden-frame")
      .setAttribute("width", "0")
      .setAttribute("height", "0")
      .allowAbbreviation(false)
      .setNewLine(false)
    ;
    iframe.setAttribute("name", iframe.getId());
    println();
    return iframe;
  }
  
  public DomBuffer makeDiv() {
    return makeTag("div");
  }

  public DomBuffer makeForm() {
    return makeTag("form");
  }

  public DomBuffer makeLi() {
    return makeTag("li");
  }
  
  public DomBuffer makeUl() {
    return makeTag("ul");
  }
  
  public DomBuffer makeOl() {
    return makeTag("ol");
  }
  
  public DomBuffer makeHeader(int h) {
    assert h > 0 && h < 7;
    return makeTag("h"+h);
  }

  public DomBuffer makeBr() {
    return makeTag("br").setNewLine(false);
  }
  
  public DomBuffer makeFieldset() {
    return makeTag("fieldset");
  }
  
  public DomBuffer makeParagraph() {
    return makeTag("p");
  }
  
  public DomBuffer makeImg() {
    return makeTag("img");
  }
  
  public DomBuffer makeInput() {
    return makeTag("input");
  }
  
  public DomBuffer makeSpan() {
    return makeTag("span");
  }
  
  public DomBuffer makeAnchor() {
    return makeTag("a");
  }
  
  public DomBuffer makeAnchorEmpty() {
    return makeTag("a").allowAbbreviation(false);
  }
  
  public DomBuffer makeAnchorInline() {
    return makeTag("a").setNewLine(false);
  }
  
  public DomBuffer makeTag(String name) {
    DomBuffer buffer = new DomBuffer(name);
    buffer.indent = indent + INDENT;
    addToEnd(buffer);
    return buffer;
  }

  public DomBuffer makeTagNoIndent(String name) {
    DomBuffer buffer = new DomBuffer(name);
    buffer.indent = indent + INDENT;
    buffer.setNewLine(false);
    addToEnd(buffer);
    return buffer;
  }

  public DomBuffer makeTagAtBeginning(String name) {
    DomBuffer buffer = new DomBuffer(name);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }
  
  public DomBuffer makeTagAtBeginningNoIndent(String name) {
    DomBuffer buffer = new DomBuffer(name);
    buffer.setNewLine(false);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  public DomBuffer setNewLine(boolean useNewLine) {
    super.setNewLine(useNewLine);
    return this;
  }

  public DomBuffer append(Object obj) {
    super.append(obj);
    return this;
  }

  public DomBuffer print(String str) {
    super.print(str);
    return this;
  }
  
  public DomBuffer clearIndent() {
    super.clearIndent();
    return this;
  }
  
  public DomBuffer append(String str) {
    super.append(str);
    return this;
  }

  public DomBuffer append(CharSequence s) {
    super.append(s);
    return this;
  }

  public DomBuffer append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return this;
  }

  public DomBuffer append(char[] str) {
    super.append(str);
    return this;
  }

  public DomBuffer append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return this;
  }

  public DomBuffer append(boolean b) {
    super.append(b);
    return this;
  }

  public DomBuffer append(char c) {
    super.append(c);
    return this;
  }

  public DomBuffer append(int i) {
    super.append(i);
    return this;
  }

  public DomBuffer append(long lng) {
    super.append(lng);
    return this;
  }

  public DomBuffer append(float f) {
    super.append(f);
    return this;
  }

  public DomBuffer append(double d) {
    super.append(d);
    return this;
  }

  public DomBuffer indent() {
    super.indent();
    return this;
  }

  public DomBuffer indentln(Object obj) {
    super.indentln(obj);
    return this;
  }

  public DomBuffer indentln(String str) {
    super.indentln(str);
    return this;
  }

  public DomBuffer indentln(CharSequence s) {
    super.indentln(s);
    return this;
  }

  public DomBuffer indentln(char[] str) {
    super.indentln(str);
    return this;
  }

  public DomBuffer outdent() {
    super.outdent();
    return this;
  }

  public DomBuffer println() {
    super.println();
    return this;
  }

  public DomBuffer println(Object obj) {
    super.println(obj);
    return this;
  }

  public DomBuffer println(String str) {
    super.println(str);
    return this;
  }

  public DomBuffer println(CharSequence s) {
    super.println(s);
    return this;
  }

  public DomBuffer println(char[] str) {
    super.println(str);
    return this;
  }

  public DomBuffer setId(String id) {
    setAttribute("id", id);
    return this;
  }

  @Override
  public DomBuffer clear() {
    super.clear();
    return this;
  }
  
  public DomBuffer allowAbbreviation(boolean abbr) {
    super.allowAbbreviation(abbr);
    return this;
  }
  
}
