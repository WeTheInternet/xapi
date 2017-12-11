package xapi.elemental;

import elemental2.core.Function;
import elemental2.core.JsObject;
import elemental2.core.Reflect;
import elemental2.dom.DocumentFragment;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.Location;
import elemental2.dom.Node;
import jsinterop.base.Js;
import xapi.elemental.api.BrowserService;
import xapi.elemental.api.ElementalIterable;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.inject.X_Inject;

import javax.inject.Provider;

public class X_Gwt3 {

  public static boolean addClassName(final Element e, final String cls) {
    if (!hasClassName(e, cls)) {
      e.className = e.className + " " + cls;
      return true;
    }
    return false;
  }

  public static Iterable<Element> attachTo(final Element body, final String html) {
    final HTMLDivElement wrapper = newDiv();
    wrapper.innerHTML = html;
    final Iterable<Element> iter = ElementalIterable.forEach(wrapper.childNodes);
    for (final Element e : iter) {
      body.appendChild(e);
    }
    return iter;
  }

  public static void attachToBody(final Element element) {
    DomGlobal.document.body.appendChild(element);
  }

  public static String concatClass(final String is, final String value) {
    final String clsName = " " + is + " ";
    return
      clsName.contains(" " + value + " ")
      ? is
        : is + " " + value;
  }

  public static void detachElement(final Element el) {
    final Node par = el.parentNode;
    if (par != null) {
      par.removeChild(el);
    }
  }

  public static native <E extends Element> E getById(String id)
  /*-{
    return $doc.getElementById(id);
  }-*/;

  public static BrowserService getElementalService() {
    return SERVICE.get();
  }

  public static String getHost() {
    final Location loc = DomGlobal.window.location;
    final JsObject target = Js.uncheckedCast(loc);
    return Reflect.get(target, "protocol") + "//" +
           Reflect.get(target, "host") + "/";
  }

  public static String getHrefById(final String id) {
    final Element el = DomGlobal.document.getElementById(id);
    return el == null
      ? ""
        : el.getAttribute("href");
  }

  public static String getInnerTextStringById(final String id) {
    final Element el = DomGlobal.document.getElementById(id);
    return el == null
      ? ""
        : el.textContent;
  }

  public static boolean hasClassName(final Element e, final String cls) {
    return (" " + e.className + " ")
      .contains(" " + cls + " ");
  }

  public static HTMLDivElement newDiv() {
    return (HTMLDivElement) X_Gwt3.DIV.cloneNode(false);
  }

  public static boolean removeClassName(final Element e, final String cls) {
    if (hasClassName(e, cls)) {
      final String clsName = " " + e.className + " ";
      e.className = clsName.replace(" " + cls + " ", " ").trim();
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked" )
  public static <E extends Element> E toElement(final String string) {
    final Element clone = (Element) X_Gwt3.DIV.cloneNode(false);
    clone.innerHTML = string;
    return (E) clone.firstElementChild;
  }

  public static DocumentFragment toFragment(final String string) {
    final DocumentFragment frag = DomGlobal.document.createDocumentFragment();
    final Element clone = (Element) X_Gwt3.DIV.cloneNode(false);
    clone.innerHTML = string;
    for (Element element : ElementalIterable.forEach(clone.childNodes)) {
      frag.appendChild(element);
    }
    return frag;
  }

  public static Iterable<Element> toElements(final String string) {
    final Element clone = (Element) X_Gwt3.DIV.cloneNode(false);
    clone.innerHTML = string;
    return ElementalIterable.forEach(clone.childNodes);
  }

  static final HTMLDivElement DIV = (HTMLDivElement) DomGlobal.document.createElement("div");

  private static final Provider<BrowserService> SERVICE = X_Inject
    .singletonLazy(BrowserService.class);

  private X_Gwt3() {}

  public static Do removeFromParentTask(Node e) {
    return In1.in1(X_Gwt3::removeFromParent).provide(e);
  }
  public static void removeFromParent(Node e) {
    if (e.parentNode != null) {
      e.parentNode.removeChild(e);
    }
  }

  public static Element getShadowRoot(HTMLElement element) {
    return getElementalService().getShadowRoot(element);
  }

  public static boolean hasShadowRoot(HTMLElement element) {
    return getElementalService().hasShadowRoot(element);
  }

  public static Element getShadowHost(HTMLElement element) {
    return getElementalService().getShadowHost(element);
  }

  private static final Function getComputedStyle =
      Js.cast(
        Reflect.get(Js.uncheckedCast(DomGlobal.window), "getComputedStyle")
      );

  public static void reflow(Element e) {
    if (e != null) {
      getComputedStyle.call(null, e); // triggers a reflow (recomputation of gui layout) for the given element.
    }
  }

  public static String escapeHTML(String html) {
    return getElementalService().escapeHTML(html);
  }

  /**
   * This method, in a browser, is fast, but insecure.
   *
   * It utilitizes a textarea's existing support for translating text into html entities,
   * and the testing done on the textarea shows that it can handle &lt;script>alert('hack')&lt;/script>,
   * without running any script.
   */
  public static String unescapeHTML(String html) {
    return getElementalService().unescapeHTML(html);
  }

  public static String getDataAttr(String ... parts) {
    StringBuilder b = new StringBuilder();
    for (String part : parts) {
      for (String chunk : part.split("-")) {
        if (chunk.length() == 0) {
          // consider warning here; the use of -- seems fishy...
          continue;
        }
        if (b.length() == 0) {
          b.append(chunk);
        } else {
          b.append(Character.toUpperCase(chunk.charAt(0)));
          if (chunk.length() > 1) {
            b.append(chunk.substring(1));
          }
        }
      }
    }
    return b.toString();
  }

  public static void insertAfter(Element newNode, Element afterNode) {
    final Node parent = afterNode.parentNode;
    assert parent != null : "You cannot perform insertAfter() with a detached element to wrap";
    final Element next = afterNode.nextElementSibling;
    if (next == null) {
      parent.appendChild(newNode);
    } else {
      parent.insertBefore(newNode, next);
    }
  }

  public static void insertAfter(Iterable<Element> newNodes, Element afterNode) {
    final Node parent = afterNode.parentNode;
    assert parent != null : "You cannot perform insertAfter() with a detached element to wrap";
    final Element next = afterNode.nextElementSibling;
    if (next == null) {
      newNodes.forEach(parent::appendChild);
    } else {
      newNodes.forEach(e->parent.insertBefore(e, afterNode));
    }
  }

  public static void ensureAttached(HTMLElement element, In1<HTMLElement> whileAttached) {
    getElementalService().ensureAttached(element, whileAttached);
  }
}
