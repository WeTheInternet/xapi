package xapi.demo.gwt.client.ui;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;


import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.StyleElement;

import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
import xapi.components.impl.GwtModelComponentMixin;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.fu.In1Out1;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtXapiTextComponent extends BaseXapiTextComponent<Node, Element, PotentialNode<Element>> implements GwtModelComponentMixin<Element,ModelXapiText> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("XapiText");
    ComponentOptions<Node, Element, GwtXapiTextComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtXapiTextComponent::new, opts);
    component.createdCallback(e->{
      final GwtXapiTextComponent c = getXapiTextComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_XAPI_TEXT = WebComponentSupport.define(
      "xapi-text", component);
  }

  private static ComponentConstructor<Node, Element, GwtXapiTextComponent> NEW_XAPI_TEXT;

  private static In1Out1<Element, GwtXapiTextComponent> getUi;

  public GwtXapiTextComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  @Override
  public PotentialNode<Element> createText () {
    return newBuilder()
      .setTagName("div")
      ;
  }

  public static GwtXapiTextComponent getXapiTextComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-text");
      final GwtXapiTextComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtXapiTextComponent create (ComponentOptions<Node, Element, GwtXapiTextComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_XAPI_TEXT.constructComponent(opts, getUi);
  }

}
