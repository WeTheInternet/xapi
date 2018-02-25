package xapi.demo.gwt.client.ui;

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
import xapi.model.api.ModelKey;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtXapiSlideComponent extends BaseXapiSlideComponent<Element, PotentialNode<Element>> implements
    ElementalModelComponentMixin<Element,ModelXapiSlide> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("XapiSlide");
    ComponentOptions<Element, GwtXapiSlideComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtXapiSlideComponent::new, opts);
    component.createdCallback(e->{
      final GwtXapiSlideComponent c = getXapiSlideComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_XAPI_SLIDE = WebComponentSupport.define(
      "xapi-slide", component);
  }

  private static ComponentConstructor<Element, GwtXapiSlideComponent> NEW_XAPI_SLIDE;

  private static In1Out1<Element, GwtXapiSlideComponent> getUi;

  public GwtXapiSlideComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  public static GwtXapiSlideComponent getXapiSlideComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-slide");
      final GwtXapiSlideComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtXapiSlideComponent create (ComponentOptions<Element, GwtXapiSlideComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_XAPI_SLIDE.constructComponent(opts, getUi);
  }

  @Override
  public PotentialNode<Element> createXapiBoxComponent (ModelKey items) {
    final PotentialNode<Element> builder = newBuilder()
      .setTagName("xapi-box");
    if (items != null) {
      if (items != null) {
        builder.setAttribute("data-model-id", items.toString());
      }
    }
    return builder;
  }

}
