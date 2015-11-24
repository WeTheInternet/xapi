package xapi.elemental;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.Location;
import xapi.elemental.api.ElementIterable;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.inject.X_Inject;
import xapi.ui.html.X_Html;
import xapi.util.X_Runtime;
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
    if (X_Runtime.isGwt()) {
      return getShadowRootNative(element);
    } else {
      return element; // no shadow roots for pure java unit tests!
    }
  }
  public static native Element getShadowRootNative(Element element)
  /*-{
      if (element.shadowRoot) {
          return element.shadowRoot;
      }
      if (element.createShadowRoot) {
          return element.createShadowRoot();
      }
      return element;
  }-*/;

  public static void reflow(Element e) {
    if (e != null) {
      Browser.getWindow().getComputedStyle(e, null); // triggers a reflow (recomputation of gui layout) for the given element.
    }
  }
}
