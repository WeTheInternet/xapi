package xapi.dev.source;

import xapi.util.api.ConvertsValue;

public class DomBuffer extends XmlBuffer {

  public DomBuffer() {
  }

  public DomBuffer(final String name) {
    super(name);
  }

  public DomBuffer setClassName(final String clsName) {
    setAttribute("class", clsName);
    return this;
  }

  public DomBuffer addClassName(final String clsName) {
    if (hasAttribute("class")) {
      setAttribute("class", getAttribute("class") + " " + clsName);
    } else {
      setAttribute("class", clsName);
    }
    return this;
  }

  public DomBuffer setAlt(final String alt) {
    setAttribute("alt", alt);
    return this;
  }

  public DomBuffer setHref(final String href) {
    setAttribute("href", href);
    return this;
  }

  public DomBuffer setSrc(final String src) {
    setAttribute("src", src);
    return this;
  }

  public DomBuffer setName(final String name) {
    setAttribute("name", name);
    return this;
  }

  public DomBuffer setType(final String type) {
    setAttribute("type", type);
    return this;
  }

  public DomBuffer setValue(final String value) {
    setAttribute("value", value);
    return this;
  }

  public DomBuffer setAction(final String action) {
    setAttribute("action", action);
    return this;
  }

  public DomBuffer setTarget(final String target) {
    setAttribute("target", target);
    return this;
  }

  public DomBuffer setMethod(final String method) {
    setAttribute("method", method);
    return this;
  }

  public DomBuffer setEnctype(final String enctype) {
    setAttribute("enctype", enctype);
    return this;
  }

  public DomBuffer setRel(final String rel) {
    setAttribute("rel", rel);
    return this;
  }

  @Override
  public DomBuffer setTagName(final String name) {
    super.setTagName(name);
    return this;
  }

  @Override
  public DomBuffer setAttribute(final String name, final String value) {
    super.setAttribute(name, value);
    return this;
  }

  public DomBuffer makeHiddenIframe() {
    final DomBuffer iframe = makeTag("iframe")
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

  public DomBuffer makeHeader(final int h) {
    assert h > 0 && h < 7;
    return makeTag("h"+h);
  }

  public DomBuffer makeBr() {
    return makeTag("br")
      .setNewLine(false)
      .allowAbbreviation(true);
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

  @Override
  public DomBuffer makeTag(final String name) {
    final DomBuffer buffer = new DomBuffer(name);
    buffer.indent = isNoTagName() ? indent : indent + INDENT;
    addToEnd(buffer);
    return buffer;
  }

  @Override
  public DomBuffer makeTagNoIndent(final String name) {
    final DomBuffer buffer = new DomBuffer(name);
    buffer.indent = indent + INDENT;
    buffer.setNewLine(false);
    addToEnd(buffer);
    return buffer;
  }

  @Override
  public DomBuffer makeTagAtBeginning(final String name) {
    final DomBuffer buffer = new DomBuffer(name);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  @Override
  public DomBuffer makeTagAtBeginningNoIndent(final String name) {
    final DomBuffer buffer = new DomBuffer(name);
    buffer.setNewLine(false);
    buffer.indent = indent + INDENT;
    addToBeginning(buffer);
    return buffer;
  }

  @Override
  public DomBuffer setNewLine(final boolean useNewLine) {
    super.setNewLine(useNewLine);
    return this;
  }

  @Override
  public DomBuffer append(final Object obj) {
    super.append(obj);
    return this;
  }

  @Override
  public DomBuffer print(final String str) {
    super.print(str);
    return this;
  }

  @Override
  public DomBuffer clearIndent() {
    super.clearIndent();
    return this;
  }

  @Override
  public DomBuffer append(final String str) {
    super.append(str);
    return this;
  }

  @Override
  public DomBuffer append(final CharSequence s) {
    super.append(s);
    return this;
  }

  @Override
  public DomBuffer append(final CharSequence s, final int start, final int end) {
    super.append(s, start, end);
    return this;
  }

  @Override
  public DomBuffer append(final char[] str) {
    super.append(str);
    return this;
  }

  @Override
  public DomBuffer append(final char[] str, final int offset, final int len) {
    super.append(str, offset, len);
    return this;
  }

  @Override
  public DomBuffer append(final boolean b) {
    super.append(b);
    return this;
  }

  @Override
  public DomBuffer append(final char c) {
    super.append(c);
    return this;
  }

  @Override
  public DomBuffer append(final int i) {
    super.append(i);
    return this;
  }

  @Override
  public DomBuffer append(final long lng) {
    super.append(lng);
    return this;
  }

  @Override
  public DomBuffer append(final float f) {
    super.append(f);
    return this;
  }

  @Override
  public DomBuffer append(final double d) {
    super.append(d);
    return this;
  }

  @Override
  public DomBuffer indent() {
    super.indent();
    return this;
  }

  @Override
  public DomBuffer indentln(final Object obj) {
    super.indentln(obj);
    return this;
  }

  @Override
  public DomBuffer indentln(final String str) {
    super.indentln(str);
    return this;
  }

  @Override
  public DomBuffer indentln(final CharSequence s) {
    super.indentln(s);
    return this;
  }

  @Override
  public DomBuffer indentln(final char[] str) {
    super.indentln(str);
    return this;
  }

  @Override
  public DomBuffer outdent() {
    super.outdent();
    return this;
  }

  @Override
  public DomBuffer println() {
    super.println();
    return this;
  }

  @Override
  public DomBuffer println(final Object obj) {
    super.println(obj);
    return this;
  }

  @Override
  public DomBuffer println(final String str) {
    super.println(str);
    return this;
  }

  @Override
  public DomBuffer println(final CharSequence s) {
    super.println(s);
    return this;
  }

  @Override
  public DomBuffer println(final char[] str) {
    super.println(str);
    return this;
  }

  @Override
  public DomBuffer setId(final String id) {
    setAttribute("id", id);
    return this;
  }

  @Override
  public DomBuffer clear() {
    super.clear();
    return this;
  }

  @Override
  public DomBuffer allowAbbreviation(final boolean abbr) {
    super.allowAbbreviation(abbr);
    return this;
  }

  public DomBuffer makeTextArea() {
    return makeTag("textarea")
      .setNewLine(false)
      .allowAbbreviation(false);
  }

  public void setPlaceholder(final String placeholder) {
    setAttribute("placeholder", placeholder);
  }

  @Override
  public DomBuffer setEscaper(final ConvertsValue<String, String> escaper) {
    super.setEscaper(escaper);
    return this;
  }

  public DomBuffer setTitle(final String title) {
    setAttribute("title", title);
    return this;
  }
}
