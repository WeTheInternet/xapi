package xapi.elemental.api;

import elemental2.dom.*;
import jsinterop.base.Js;
import xapi.elemental.X_Gwt3;
import xapi.fu.In1;
import xapi.ui.api.*;
import xapi.util.X_Debug;

import java.util.function.BiFunction;

import static xapi.fu.Immutable.immutable1;

public class ElementalBuilder<E extends Node> extends ElementBuilder<E> implements CreatesChildren<String, ElementalBuilder<E>> {

  public class ApplyLiveAttribute implements AttributeApplier {

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

  public ElementalBuilder() {
    this(false);
  }

  public ElementalBuilder(boolean searchableChildren) {
    super(searchableChildren);
    setDefaultFactories();
  }

  public ElementalBuilder(String tagName) {
    super(tagName);
  }

  public ElementalBuilder(String tagName, boolean searchableChildren) {
    super(tagName, searchableChildren);
  }

  public ElementalBuilder(E element) {
    super(element);
  }

  @Override
  protected BiFunction<String, Boolean, NodeBuilder<E>> getCreator() {
    return ElementalBuilder::new; // pick the constructor you like
  }

  @Override
  protected AttributeApplier createAttributeApplier() {
    return el == null ? new ApplyPendingAttribute() : new ApplyLiveAttribute();
  }

  public ElementalBuilder<E> createNode(String tagName) {
    final NodeBuilder<E> child = newNode.apply(tagName, searchableChildren);
    assert child instanceof ElementalBuilder :
        "A potential node cannot have a factory which does not supply a new ElementalBuilder" ;
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
      attributeApplier = immutable1(new ApplyLiveAttribute());
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

  /**
   * @param tagName the tagName to set
   */
  public ElementalBuilder<E> setTagName(String tagName) {
    super.setTagName(tagName);
    return this;
  }


  @Override
  public String toString() {
    final E ele = getElement();
    return ele == null ? "" : Js.asPropertyMap(ele).getAny("outerHTML").asString();
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
    return "#document-fragment".equals(el.nodeName);
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

    @Override
    protected void clearChildren(E element) {
        element.textContent = "";
    }

  @Override
  public ElementalBuilder<E> onCreated(In1<E> callback) {
    super.onCreated(callback);
    return this;
  }

  @Override
  public ElementalBuilder<E> append(CharSequence chars) {
    super.append(chars);
    return this;
  }
}
