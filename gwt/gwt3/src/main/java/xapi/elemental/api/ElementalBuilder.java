package xapi.elemental.api;

import elemental2.dom.DocumentFragment;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import jsinterop.base.Js;
import xapi.elemental.X_Gwt3;
import xapi.ui.api.*;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.impl.ImmutableProvider;

import java.util.function.BiFunction;

public class ElementalBuilder<E extends Node> extends ElementBuilder<E> implements CreatesChildren<String, ElementalBuilder<E>> {

  public class ApplyLiveAttribute implements AttributeApplier {

    @Override
    public void addAttribute(String name, String value) {
      String is = getAttribute(name);
      if (X_String.isEmpty(is)) {
        setAttribute(name, value);
      } else {
        setAttribute(name, concat(name, is, value));
      }
    }

    protected String concat(String name, String is, String value) {
      return "class".equals(name) ?
          X_Gwt3.concatClass(is, value)
          : is.concat(value);
    }

    @Override
    public void setAttribute(String name, String value) {
      getAsElement().setAttribute(name, value);
    }

    @Override
    public String getAttribute(String name) {
      return getAsElement().getAttribute(name);
    }

    @Override
    public void removeAttribute(String name) {
      getAsElement().removeAttribute(name);
    }

  }

  protected Element getAsElement() {
    final E element = getElement();
    if (element.nodeType != Node.ELEMENT_NODE) {
      assert false : "Node " + element.nodeName + " does not support attributes";
      throw X_Debug.recommendAssertions();
    }
    return (Element) element;
  }

  private String tagName;

  public ElementalBuilder() {
    this(false);
  }

  public ElementalBuilder(boolean searchableChildren) {
    super(searchableChildren);
    setDefaultFactories();
  }

  public ElementalBuilder(String tagName) {
    this();
    setTagName(tagName);
  }

  public ElementalBuilder(String tagName, boolean searchableChildren) {
    super(searchableChildren);
    setDefaultFactories();
    setTagName(tagName);
  }

  public ElementalBuilder(E element) {
    super(false);
    setDefaultFactories();
    el = element;
    onInitialize(el);
  }

  @Override
  protected BiFunction<String, Boolean, NodeBuilder<E>> getCreator() {
    return ElementalBuilder::new; // pick the constructor you like
  }

  @Override
  protected void toHtml(Appendable out) {
    if (tagName != null) {
      // If we have a tagname, then we might expect our element to be addressable.
      // In which case, we want to ensure it has an id
      if (searchableChildren) {
        ensureId();
      }
    }
    super.toHtml(out);
  }

  @Override
  protected AttributeApplier createAttributeApplier() {
    return el == null ? new ApplyPendingAttribute() : new ApplyLiveAttribute();
  }

  public ElementalBuilder<E> createNode(String tagName) {
    final NodeBuilder<E> child = newNode.apply(tagName, searchableChildren);
    assert child instanceof ElementalBuilder :
        "A potential node cannot have a factory which does not supply a new potential node" ;
    return (ElementalBuilder<E>) child;
  }

  public ElementalBuilder<E> createChild(String tagName, String is) {
    final ElementalBuilder<E> child = createChild(tagName);
    child.setAttribute("is", is);
    return child;
  }

  @Override
  public ElementalBuilder<E> createChild(String tagName) {
    return (ElementalBuilder<E>) super.createChild(tagName);
  }

  @Override
  protected StyleApplier createStyleApplier() {
    return new Styler();
  }

  @Override
  protected E create(CharSequence csq) {
    try {
      E e = build(csq.toString());
      return e;
    } finally {
      attributeApplier = new ImmutableProvider<>(new ApplyLiveAttribute());
    }
  }

  protected E build(String html) {
    boolean isMulti = children != null && children.hasSiblings();
    if (isMulti) {
      final String toAppend = sanitizeHtml(html).trim();
      final DocumentFragment frag = X_Gwt3.toFragment(toAppend);
      if (compressFragments()) {
        return compressFragment(frag, html);
      }
      return Js.uncheckedCast(frag);
    }
    Element e = X_Gwt3.toElement(sanitizeHtml(html));
    return (E) e;
  }

  protected boolean compressFragments() {
    return false;
  }

  protected E compressFragment(DocumentFragment frag, String html) {
    final Node node = frag.firstChild;
    if (node.nodeType == Node.ELEMENT_NODE) {
      Element e = (Element) frag.firstChild;
      frag.removeChild(e);
      e.appendChild(frag);
      return (E)e;
    } else {
      assert false : "Cannot compress html into a single node; first child must be an element. Html:\n" + html;
      throw X_Debug.recommendAssertions();
    }
  }

  protected String sanitizeHtml(String html) {
    return bodySanitizer.apply(html); // TODO: actually sanitize
  }

  @Override
  public void append(Widget<E> child) {
    getElement().appendChild(child.getElement());
  }

  @Override
  protected NodeBuilder<E> wrapChars(CharSequence body) {
    ElementalBuilder<E> node = new ElementalBuilder<>();
    node.append(body);
    return node;
  }

  @Override
  protected CharSequence getCharsBefore() {
    StringBuilder b = new StringBuilder();
    if (tagName == null || tagName.isEmpty()) {
      assert attributes.isEmpty() : "Cannot have attributes without a tagname";
    } else {
      b.append("<");
      b.append(tagName);
      appendAttributes(b);
      if (isTagEmpty()) {
        b.append("/");
      }
      b.append(">");
    }
    return b.length() == 0 ? EMPTY : b.toString();
  }

  @Override
  protected boolean isTagEmpty() {
    if (super.isTagEmpty()) {
      switch(tagName.toLowerCase()) {
        case "hr":
        case "br":
        case "img":
        case "input":
          // Any elements which should not be created with closing tags
          return true;
      }
    }
    return false;
  }

  private void appendAttributes(StringBuilder b) {
    if (!attributes.isEmpty()) {
      for (String attribute : attributes.keys()) {
        b.append(" ").append(attribute).append("=\"");
        String result = attributes.get(attribute).getElement();
        b.append(sanitizeAttribute(result)).append("\"");
      }
    }
  }

  protected String sanitizeAttribute(String result) {
    return  X_String.isEmpty(result) ? "" : result.replaceAll("\"", "&quot;");
  }

  @Override
  protected CharSequence getCharsAfter(CharSequence self) {
    if (tagName != null && !isTagEmpty()) {
      return "</"+tagName+">";
    }
    return EMPTY;
  }

  /**
   * @return the tagName
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * @param tagName the tagName to set
   */
  public ElementalBuilder<E> setTagName(String tagName) {
    this.tagName = tagName;
    return this;
  }

  public String toSource() {
    StringBuilder b = new StringBuilder();
    toHtml(b);
    return sanitizeHtml(b.toString());
  }

  @Override
  public String toString() {
    final E ele = getElement();
    return ele == null ? "" : Js.asPropertyMap(ele).getAny("outerHTML").asString();
  }

  @Override
  public StyleApplier getStyle() {
    if (X_String.isEmpty(getTagName()) && children instanceof ElementBuilder) {
      return ((ElementBuilder)children).getStyle();
    }
    return super.getStyle();
  }

  @Override
  public ElementBuilder<E> setStyle(String name, String value) {
    return super.setStyle(name, value);
  }

  class Styler extends StyleApplier {

    @Override
    protected void removeStyle(E element, String key) {
      ((HTMLElement)element).style.removeProperty(key);
    }

    @Override
    protected void setStyle(E element, String key, String value) {
      ((HTMLElement)element).style.setProperty(key, value);
    }
  }

  @Override
  protected E findSelf(E parent) {
    if (el != null) {
      return el;
    }
    // First, look on the document.  This is the fastest, IF we are attached
    String id = getId();
    if (id == null) {
      return super.findSelf(parent);
    }
    el = (E) DomGlobal.document.getElementById(id);
    if (el == null && parent != null) {
      // If we aren't attached, fallback to the slower querySelector
      el = (E) parent.querySelector("#"+id);
    }
    if (el != null) {
      return el;
    }
    return super.findSelf(parent);
  }

  @Override
  protected void startInitialize(E el) {
    if (isAutoAppend(el)) {
      DomGlobal.document.body.appendChild(el);
    }
    super.startInitialize(el);
  }

  protected boolean isAutoAppend(E el) {
    return false;
  }

  @Override
  protected void finishInitialize(E el) {
    if (isAutoAppend(el)) {
      final Element body = DomGlobal.document.body;
      if (body == el.parentNode) {
        body.removeChild(el);
      }
    }
    super.finishInitialize(el);
  }
}
