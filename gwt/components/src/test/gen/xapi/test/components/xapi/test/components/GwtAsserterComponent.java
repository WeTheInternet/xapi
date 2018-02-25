package xapi.test.components.xapi.test.components;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;


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

public class GwtAsserterComponent extends BaseAsserterComponent<Element, PotentialNode<Element>> implements
    ElementalModelComponentMixin<Element,ModelAsserter> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("Asserter");
    ComponentOptions<Element, GwtAsserterComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtAsserterComponent::new, opts);
    component.createdCallback(e->{
      final GwtAsserterComponent c = getAsserterComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_XAPI_ASSERTER = WebComponentSupport.define(
      "xapi-asserter", component);
  }

  private static ComponentConstructor<Element, GwtAsserterComponent> NEW_XAPI_ASSERTER;

  private static In1Out1<Element, GwtAsserterComponent> getUi;

  public GwtAsserterComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  public static GwtAsserterComponent getAsserterComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-asserter");
      final GwtAsserterComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtAsserterComponent create (ComponentOptions<Element, GwtAsserterComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_XAPI_ASSERTER.constructComponent(opts, getUi);
  }

}
