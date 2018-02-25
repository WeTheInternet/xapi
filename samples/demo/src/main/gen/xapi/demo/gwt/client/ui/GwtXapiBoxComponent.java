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
import xapi.model.service.ModelCache;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtXapiBoxComponent extends BaseXapiBoxComponent<Element, PotentialNode<Element>> implements
    ElementalModelComponentMixin<Element,ModelXapiBox> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("XapiBox");
    ComponentOptions<Element, GwtXapiBoxComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtXapiBoxComponent::new, opts);
    component.createdCallback(e->{
      final GwtXapiBoxComponent c = getXapiBoxComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_XAPI_BOX = WebComponentSupport.define(
      "xapi-box", component);
  }

  private static ComponentConstructor<Element, GwtXapiBoxComponent> NEW_XAPI_BOX;

  private static In1Out1<Element, GwtXapiBoxComponent> getUi;

  public GwtXapiBoxComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  public static GwtXapiBoxComponent getXapiBoxComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-box");
      final GwtXapiBoxComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtXapiBoxComponent create (ComponentOptions<Element, GwtXapiBoxComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_XAPI_BOX.constructComponent(opts, getUi);
  }

  @Override
  public PotentialNode<Element> createXapiTextComponent (ModelXapiText text) {
    final PotentialNode<Element> builder = newBuilder()
      .setTagName("xapi-text");
    if (text != null) {
      ModelCache cache = cache();
      ModelKey key = cache.ensureKey(ModelXapiText.MODEL_XAPI_TEXT, text);
      cache.cacheModel(text, ignore->{});
      if (key != null) {
        builder.setAttribute("data-model-id", key.toString());
      }
    }
    return builder;
  }

}
