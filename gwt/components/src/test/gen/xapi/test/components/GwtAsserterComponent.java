package xapi.test.components;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;


import elemental.dom.Element;
import elemental.html.StyleElement;

import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
import xapi.components.impl.ElementalModelComponentMixin;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.elemental.api.Elemental1Injector;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.fu.api.Builderizable;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtAsserterComponent extends BaseAsserterComponent<Element, PotentialNode<Element>> implements Builderizable<PotentialNode<Element>>, ElementalModelComponentMixin<Element,ModelAsserter> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    if (NEW_XAPI_ASSERTER != null) { return; }
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("Asserter");
    ModelComponentOptions<Element, ModelAsserter, GwtAsserterComponent> opts = new ModelComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtAsserterComponent::new, opts);
    component.afterCreatedCallback(e->{
      final GwtAsserterComponent c = getAsserterComponent(e);
      c.getElement(); // ensure ui is initialized
    });
    NEW_XAPI_ASSERTER = WebComponentSupport.define(
      "xapi-asserter", component);
  }

  public static BuildAsserterComponent builder () {
    return new BuildAsserterComponent<>(NEW_XAPI_ASSERTER, getUi);
  }

  private static ComponentConstructor<Element, GwtAsserterComponent> NEW_XAPI_ASSERTER;

  private static In1Out1<Element, GwtAsserterComponent> getUi;

  public GwtAsserterComponent (Element el) {
    super(el);
  }

  @SuppressWarnings("unchecked")
  public GwtAsserterComponent (ModelComponentOptions<Element, ModelAsserter, AsserterComponent<Element>> opts) {
    super(opts, (ComponentConstructor)NEW_XAPI_ASSERTER);
  }

  public PotentialNode<Element> newBuilder (boolean searchable) {
    return new PotentialNode<Element>(searchable);
  }

  public Elemental1Injector newInjector (Element el) {
    return new Elemental1Injector(el);
  }

  public static GwtAsserterComponent getAsserterComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("xapi-asserter");
      final GwtAsserterComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtAsserterComponent create (ModelComponentOptions<Element, ModelAsserter, GwtAsserterComponent> opts) {
    if (opts == null) {
      opts = new ModelComponentOptions<>();
    }
    return NEW_XAPI_ASSERTER.constructComponent(opts, getUi);
  }

  @Override
  public PotentialNode<Element> asBuilder () {
    return (PotentialNode<Element>) super.asBuilder();
  }

  @Override
  protected PotentialNode<Element> newBuilder (Out1<Element> e) {
    return new PotentialNode<>(e);
  }

}
