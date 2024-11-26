package xapi.ui.layout;

import elemental.dom.Element;
import elemental.html.StyleElement;
import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
import xapi.components.impl.ElementalModelComponentMixin;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.fu.In1Out1;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;

public class GwtBoxComponent extends BaseBoxComponent<Element, PotentialNode<Element>> implements
    ElementalModelComponentMixin<Element,ModelBox> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("Box");
    ComponentOptions<Element, GwtBoxComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtBoxComponent::new, opts);
    component.createdCallback(e->{
      final GwtBoxComponent c = getBoxComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_BOX = WebComponentSupport.define(
      "box", component);
  }

  private static ComponentConstructor<Element, GwtBoxComponent> NEW_BOX;

  private static In1Out1<Element, GwtBoxComponent> getUi;

  public GwtBoxComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  @Override
  public PotentialNode<Element> createRoot () {
    return newBuilder()
      .setTagName("div")
      ;
  }

  public static GwtBoxComponent getBoxComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("box");
      final GwtBoxComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtBoxComponent create (ComponentOptions<Element, GwtBoxComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_BOX.constructComponent(opts, getUi);
  }

  public Element getFirstChild() {
    return getElement().getFirstElementChild();
  }

}
