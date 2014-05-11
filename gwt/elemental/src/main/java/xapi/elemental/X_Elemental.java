package xapi.elemental;

import javax.inject.Provider;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.Location;
import xapi.elemental.api.ElementIterable;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.inject.X_Inject;
import xapi.util.api.ConvertsValue;

public class X_Elemental {

  static final DivElement DIV = Browser.getDocument().createDivElement();

  private X_Elemental() {}

  private static final Provider<ElementalService> SERVICE = X_Inject
      .singletonLazy(ElementalService.class);

  public static <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(
      final Class<T> cls) {
    return SERVICE.get().toElementBuilder(cls);
  }

  public static <T, E extends Element> E toElement(
      final Class<? super T> cls,
      T obj) {
    return SERVICE.get().toElement(cls, obj);
  }

  public static <T, E extends Element> ConvertsValue<T, PotentialNode<E>> toElementBuilder(
      final Class<T> model, final Class<?> template) {
    return SERVICE.get().toElementBuilder(model, template);
  }

  public static <T, E extends Element> E toElement(
      final Class<? super T> model, final Class<?> template, T obj) {
    return SERVICE.get().toElement(model, template, obj);
  }

  public static ElementalService getElementalService() {
    return SERVICE.get();
  }

  public static void addClassName(Element e, String cls) {
    String clsName = " " + e.getClassName() + " ";
    if (!clsName.contains(" " + cls + " ")) {
      e.setClassName(e.getClassName() + " " + cls);
    }
  }

  public static void removeClassName(Element e, String cls) {
    String clsName = " " + e.getClassName() + " ";
    e.setClassName(clsName.replace(" " + cls + " ", " ").trim());
  }

  public static String getHrefById(String id) {
    Element el = Browser.getDocument().getElementById(id);
    return el == null
        ? ""
        : el.getAttribute("href");
  }

  @SuppressWarnings("unchecked" )
  public static <E extends Element> E toElement(String string) {
    Element clone = (Element) X_Elemental.DIV.cloneNode(false);
    clone.setInnerHTML(string);
    return (E) clone.getFirstElementChild();
  }

  public static Iterable<Element> toElements(String string) {
    Element clone = (Element) X_Elemental.DIV.cloneNode(false);
    clone.setInnerHTML(string);
    return ElementIterable.forEach(clone.getChildNodes());
  }

  public static native <E extends Element> E getById(String id)
  /*-{
    return $doc.getElementById(id);
  }-*/;

  public static Element newDiv() {
    return (Element) X_Elemental.DIV.cloneNode(false);
  }

  public static void alert(String msg) {
    Browser.getWindow().alert(msg);
  }

  public static Iterable<Element> attachTo(Element body, String html) {
    DivElement wrapper = Browser.getDocument().createDivElement();
    wrapper.setInnerHTML(html);
    Iterable<Element> iter = ElementIterable.forEach(wrapper.getChildren());
    for (Element e : iter) {
      body.appendChild(e);
    }
    return iter;
  }

  public static String getInnerTextStringById(String id) {
    Element el = Browser.getDocument().getElementById(id);
    return el == null
        ? ""
        : el.getInnerText();
  }

  public static String getHost() {
    Location loc = Browser.getWindow().getLocation();
    return loc.getProtocol() + "//" + loc.getHost() + "/";
  }

  public static void detachElement(Element el) {
    Element par = el.getParentElement();
    if (par != null) {
      par.removeChild(el);
    }
  }

  public static void attachToBody(Element element) {
    Browser.getDocument().getBody().appendChild(element);
  }

  public static String concatClass(String is, String value) {
    String clsName = " " + is + " ";
    return
        clsName.contains(" " + value + " ")
        ? is
        : is + " " + value;
  }

}
