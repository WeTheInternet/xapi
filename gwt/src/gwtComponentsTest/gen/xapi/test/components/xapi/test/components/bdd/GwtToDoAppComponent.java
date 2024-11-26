package xapi.test.components.xapi.test.components.bdd;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;


import elemental.dom.Element;
import elemental.html.StyleElement;

import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
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
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtToDoAppComponent extends BaseToDoAppComponent<Element, PotentialNode<Element>> implements Builderizable<PotentialNode<Element>> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    if (NEW_TODOAPP != null) { return; }
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("ToDoApp");
    ComponentOptions<Element, GwtToDoAppComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtToDoAppComponent::new, opts);
    component.afterCreatedCallback(e->{
      final GwtToDoAppComponent c = getToDoAppComponent(e);
      c.getElement(); // ensure ui is initialized
    });
    NEW_TODOAPP = WebComponentSupport.define(
      "null", component);
  }

  public static BuildToDoAppComponent builder () {
    return new BuildToDoAppComponent<>(NEW_TODOAPP, getUi);
  }

  private static ComponentConstructor<Element, GwtToDoAppComponent> NEW_TODOAPP;

  private static In1Out1<Element, GwtToDoAppComponent> getUi;

  public GwtToDoAppComponent (Element el) {
    super(el);
  }

  @SuppressWarnings("unchecked")
  public GwtToDoAppComponent (ComponentOptions<Element, ToDoAppComponent<Element>> opts) {
    super(opts, (ComponentConstructor)NEW_TODOAPP);
  }

  public PotentialNode<Element> newBuilder (boolean searchable) {
    return new PotentialNode<Element>(searchable);
  }

  public Elemental1Injector newInjector (Element el) {
    return new Elemental1Injector(el);
  }

  public static GwtToDoAppComponent getToDoAppComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("ToDoApp");
      final GwtToDoAppComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtToDoAppComponent create (ComponentOptions<Element, GwtToDoAppComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_TODOAPP.constructComponent(opts, getUi);
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
