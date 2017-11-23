package xapi.elemental.api;

import elemental2.dom.DocumentFragment;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import jsinterop.base.JsPropertyMap;
import xapi.elemental.X_Gwt3;
import xapi.ui.api.AttributeApplier;
import xapi.ui.api.ElementBuilder;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.Widget;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.impl.ImmutableProvider;

import java.util.function.BiFunction;

/**
 * TODO: rename this to ElementalBuilder?
 */
public class ElementalBuilder<E extends HTMLElement> extends ElementBuilder<E> {

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
      getElement().setAttribute(name, value);
    }

    @Override
    public String getAttribute(String name) {
      return getElement().getAttribute(name);
    }

    @Override
    public void removeAttribute(String name) {
      getElement().removeAttribute(name);
    }

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
    final ElementalBuilder<E> child = createNode(tagName);
    addChild(child, X_String.isEmpty(tagName)); // If we are a document fragment, we need to make new children the target element for future inserts
    return child;
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
      final DocumentFragment frag = X_Gwt3.toFragment(sanitizeHtml(html).trim());
      return compressFragment(frag, html);
    }
    return X_Gwt3.toElement(sanitizeHtml(html));
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
      if (isEmpty()) {
        b.append("/");
      }
      b.append(">");
    }
    return b.length() == 0 ? EMPTY : b.toString();
  }

  @Override
  protected boolean isEmpty() {
    if (super.isEmpty()) {
      switch(tagName.toLowerCase()) {
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
        b.append(" ").append(attribute).append("='");
        String result = attributes.get(attribute).getElement();
        b.append(sanitizeAttribute(result)).append("'");
      }
    }
  }

  protected String sanitizeAttribute(String result) {
    return  X_String.isEmpty(result) ? "" : result.replaceAll("'", "&apos;");
  }

  @Override
  protected CharSequence getCharsAfter(CharSequence self) {
    if (tagName != null && !isEmpty()) {
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
    return ele == null ? "" : (String) JsPropertyMap.of(ele).get("outerHTML");
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
      element.style.removeProperty(key);
    }

    @Override
    protected void setStyle(E element, String key, String value) {
      element.style.setProperty(key, value);
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
    DomGlobal.document.body.appendChild(el);
    super.startInitialize(el);
  }

  @Override
  protected void finishInitialize(E el) {
    final Element body = DomGlobal.document.body;
    if (body == el.parentNode) {
      body.removeChild(el);
    }
    super.finishInitialize(el);
  }
}
