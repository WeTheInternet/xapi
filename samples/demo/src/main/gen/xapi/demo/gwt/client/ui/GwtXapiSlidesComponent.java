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
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelCache;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtXapiSlidesComponent extends BaseXapiSlidesComponent<Node, Element, PotentialNode<Element>> implements GwtModelComponentMixin<Element,ModelXapiSlides> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("XapiSlides");
    ComponentOptions<Node, Element, GwtXapiSlidesComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtXapiSlidesComponent::new, opts);
    component.createdCallback(e->{
      final GwtXapiSlidesComponent c = getXapiSlidesComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    component.afterCreatedCallback(getUi, GwtXapiSlidesComponent::created);
    NEW_XAPI_SLIDES = WebComponentSupport.define(
      "xapi-slides", component);
  }

  private static ComponentConstructor<Node, Element, GwtXapiSlidesComponent> NEW_XAPI_SLIDES;

  private static In1Out1<Element, GwtXapiSlidesComponent> getUi;

  public GwtXapiSlidesComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  public static GwtXapiSlidesComponent getXapiSlidesComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-slides");
      final GwtXapiSlidesComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtXapiSlidesComponent create (ComponentOptions<Node, Element, GwtXapiSlidesComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_XAPI_SLIDES.constructComponent(opts, getUi);
  }

  @Override
  public PotentialNode<Element> createXapiSlideComponent (ModelXapiSlide showing) {
    final PotentialNode<Element> builder = newBuilder()
      .setTagName("xapi-slide");
    if (showing != null) {
      ModelCache cache = cache();
      ModelKey key = cache.ensureKey(ModelXapiSlide.MODEL_XAPI_SLIDE, showing);
      cache.cacheModel(showing, ignore->{});
      if (key != null) {
        builder.setAttribute("data-model-id", key.toString());
      }
    }
    return builder;
  }
  public void created() {
    String id = getModelId(getElement());
    xapi.log.X_Log.info("My model id is: ", id);
    ModelKey key = ModelXapiSlides.newKey().withId(id).buildKey();
    ModelXapiSlides cached = (ModelXapiSlides) cache().getModel(key);
    if (cached != null) {
      getModel().absorb(cached);
    }
  }

}
