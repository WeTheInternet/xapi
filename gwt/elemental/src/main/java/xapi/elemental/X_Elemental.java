package xapi.elemental;

import elemental.client.Browser;
import elemental.dom.DocumentFragment;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.Location;
import xapi.elemental.api.ElementIterable;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.inject.X_Inject;
import xapi.ui.html.X_Html;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.client.MagicMethod;

import javax.inject.Provider;

public class X_Elemental {

  public static void addClassName(final Element e, final String cls) {
    if (!hasClassName(e, cls)) {
      e.setClassName(e.getClassName() + " " + cls);
    }
  }

  public static void alert(final String msg) {
    Browser.getWindow().alert(msg);
  }

  public static Iterable<Element> attachTo(final Element body, final String html) {
    final DivElement wrapper = Browser.getDocument().createDivElement();
    wrapper.setInnerHTML(html);
    final Iterable<Element> iter = ElementIterable.forEach(wrapper.getChildren());
    for (final Element e : iter) {
      body.appendChild(e);
    }
    return iter;
  }

  public static void attachToBody(final Element element) {
    Browser.getDocument().getBody().appendChild(element);
  }

  public static String concatClass(final String is, final String value) {
    final String clsName = " " + is + " ";
    return
      clsName.contains(" " + value + " ")
      ? is
        : is + " " + value;
  }

  public static void detachElement(final Element el) {
    final Element par = el.getParentElement();
    if (par != null) {
      par.removeChild(el);
    }
  }

  public static native <E extends Element> E getById(String id)
  /*-{
    return $doc.getElementById(id);
  }-*/;

  public static ElementalService getElementalService() {
    return SERVICE.get();
  }

  public static String getHost() {
    final Location loc = Browser.getWindow().getLocation();
    return loc.getProtocol() + "//" + loc.getHost() + "/";
  }

  public static String getHrefById(final String id) {
    final Element el = Browser.getDocument().getElementById(id);
    return el == null
      ? ""
        : el.getAttribute("href");
  }

  public static String getInnerTextStringById(final String id) {
    final Element el = Browser.getDocument().getElementById(id);
    return el == null
      ? ""
        : el.getInnerText();
  }

  public static boolean hasClassName(final Element e, final String cls) {
    return (" " + e.getClassName() + " ")
      .contains(" " + cls + " ");
  }

  @MagicMethod(doNotVisit=true)
  public static void injectCss(final Class<?> cls) {
    X_Html.injectCss(cls, SERVICE.get());
  }

  public static Element newDiv() {
    return (Element) X_Elemental.DIV.cloneNode(false);
  }

  public static void removeClassName(final Element e, final String cls) {
    final String clsName = " " + e.getClassName() + " ";
    e.setClassName(clsName.replace(" " + cls + " ", " ").trim());
  }

  @MagicMethod(doNotVisit=true)
  public static <T, E extends Element> E toElement(
    final Class<? super T> model, final Class<?> template, final T obj) {
    return SERVICE.get().toElement(model, template, obj);
  }

  @MagicMethod(doNotVisit=true)
  public static <T, E extends Element> E toElement(
    final Class<? super T> cls,
    final T obj) {
    return SERVICE.get().toElement(cls, obj);
  }

  @SuppressWarnings("unchecked" )
  public static <E extends Element> E toElement(final String string) {
    final Element clone = (Element) X_Elemental.DIV.cloneNode(false);
    clone.setInnerHTML(string);
    return (E) clone.getFirstElementChild();
  }

  public static DocumentFragment toFragment(final String string) {
    final DocumentFragment frag = Browser.getDocument().createDocumentFragment();
    final Element clone = (Element) X_Elemental.DIV.cloneNode(false);
    clone.setInnerHTML(string);
    for (Element element : ElementIterable.forEach(clone.getChildren())) {
      frag.appendChild(element);
    }
    return frag;
  }

  @MagicMethod(doNotVisit=true)
  public static <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(
    final Class<T> cls) {
    return SERVICE.get().toElementBuilder(cls);
  }

  @MagicMethod(doNotVisit=true)
  public static <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(
    final Class<T> model, final Class<?> template) {
    return SERVICE.get().toElementBuilder(model, template);
  }

  public static Iterable<Element> toElements(final String string) {
    final Element clone = (Element) X_Elemental.DIV.cloneNode(false);
    clone.setInnerHTML(string);
    return ElementIterable.forEach(clone.getChildNodes());
  }

  static final DivElement DIV = Browser.getDocument().createDivElement();

  private static final Provider<ElementalService> SERVICE = X_Inject
    .singletonLazy(ElementalService.class);

  private X_Elemental() {}

  public static void removeFromParent(Element e) {
    if (e.getParentElement() != null) {
      e.getParentElement().removeChild(e);
    }
  }

  public static Element getShadowRoot(Element element) {
    return getElementalService().getShadowRoot(element);
  }

  public static void reflow(Element e) {
    if (e != null) {
      Browser.getWindow().getComputedStyle(e, null); // triggers a reflow (recomputation of gui layout) for the given element.
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
    final Element parent = afterNode.getParentElement();
    assert parent != null : "You cannot perform insertAfter() with a detached element to wrap";
    final Element next = afterNode.getNextElementSibling();
    if (next == null) {
      parent.appendChild(newNode);
    } else {
      parent.insertBefore(newNode, next);
    }
  }
}
