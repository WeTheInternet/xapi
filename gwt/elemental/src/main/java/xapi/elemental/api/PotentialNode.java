package xapi.elemental.api;

import elemental.client.Browser;
import elemental.dom.DocumentFragment;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental2.dom.DomGlobal;
import xapi.elemental.X_Elemental;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.api.*;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.ImmutableProvider;

import java.util.function.BiFunction;

/**
 * TODO: rename this to ElementalBuilder?
 */
public class PotentialNode <E extends Element> extends ElementBuilder<E> implements CreatesChildren<String, PotentialNode<E>> {

  public class ApplyLiveAttribute implements AttributeApplier {

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

  private Lazy<Element> attachTo = Lazy.deferred1(Browser.getDocument()::getBody);

  public PotentialNode() {
    this(false);
  }

  public PotentialNode(boolean searchableChildren) {
    super(searchableChildren);
    setDefaultFactories();
  }

  public PotentialNode(String tagName) {
    super(tagName);
  }

  public PotentialNode(String tagName, boolean searchableChildren) {
    super(searchableChildren);
    setDefaultFactories();
    setTagName(tagName);
  }

  public PotentialNode(E element) {
    super(false);
    setDefaultFactories();
    el = element;
    onInitialize(el);
  }

  @Override
  protected BiFunction<String, Boolean, NodeBuilder<E>> getCreator() {
    return PotentialNode::new; // pick the constructor you like
  }

  @Override
  protected void toHtml(Appendable out) {
    if (getTagName() != null) {
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

  @Override
  public PotentialNode<E> createNode(String tagName) {
    final NodeBuilder<E> child = newNode.apply(tagName, searchableChildren);
    assert child instanceof PotentialNode :
        "A potential node cannot have a factory which does not supply a new potential node" ;
    return (PotentialNode<E>) child;
  }

  public PotentialNode<E> createChild(String tagName, String is) {
    final PotentialNode<E> child = createChild(tagName);
    child.setAttribute("is", is);
    return child;
  }

  @Override
  public PotentialNode<E> createChild(String tagName) {
    return (PotentialNode<E>) super.createChild(tagName);
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
      final DocumentFragment frag = X_Elemental.toFragment(sanitizeHtml(html).trim());
      return compressFragment(frag, html);
    }
    return X_Elemental.toElement(sanitizeHtml(html));
  }

  protected E compressFragment(DocumentFragment frag, String html) {
    final Node node = frag.getFirstChild();
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element e = (Element) frag.getFirstChild();
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
    PotentialNode<E> node = new PotentialNode<>();
    node.append(body);
    return node;
  }

  @Override
  public String toSource() {
    StringBuilder b = new StringBuilder();
    toHtml(b);
    return sanitizeHtml(b.toString());
  }

  @Override
  public String toString() {
    final E ele = getElement();
    return ele == null ? "" : ele.getOuterHTML();
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
      element.getStyle().removeProperty(key);
    }

    @Override
    protected void setStyle(E element, String key, String value) {
      element.getStyle().setProperty(key, value);
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
    el = (E) Browser.getDocument().getElementById(id);
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
    attachTo.out1().appendChild(el);
    super.startInitialize(el);
  }

  @Override
  protected void finishInitialize(E el) {
    final Element body = attachTo.out1();
    if (body == DomGlobal.document.body && body == el.getParentElement() && !"true".equals(el.getAttribute("no-remove"))) {
      body.removeChild(el);
    }
    super.finishInitialize(el);
  }

  @Override
  protected void clearChildren(E element) {
    // lazy...
    element.setTextContent("");
  }

  @Override
  public PotentialNode<E> setTagName(String tagName) {
    super.setTagName(tagName);
    return this;
  }

  @Override
  public PotentialNode<E> setAttribute(String name, String value) {
    super.setAttribute(name, value);
    return this;
  }

    @Override
    public PotentialNode<E> onCreated(ReceivesValue<E> callback) {
        super.onCreated(callback);
        return this;
    }

  @Override
  public PotentialNode<E> append(CharSequence chars) {
    super.append(chars);
    return this;
  }

  public PotentialNode<E> attachTo(E el) {
    attachTo = Lazy.immutable1(el);
    return this;
  }

  public PotentialNode<E> attachTo(Out1<Element> el) {
    attachTo = Lazy.deferred1(el);
    return this;
  }
}
