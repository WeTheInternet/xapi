package xapi.ui.edit;

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

public class GwtInputTextComponent extends BaseInputTextComponent<Element, PotentialNode<Element>> implements
    ElementalModelComponentMixin<Element,ModelInputText> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("InputText");
    ComponentOptions<Element, GwtInputTextComponent> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtInputTextComponent::new, opts);
    component.createdCallback(e->{
      final GwtInputTextComponent c = getInputTextComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    NEW_INPUT_TEXT = WebComponentSupport.define(
      "input-text", component);
  }

  private static ComponentConstructor<Element, GwtInputTextComponent> NEW_INPUT_TEXT;

  private static In1Out1<Element, GwtInputTextComponent> getUi;

  public GwtInputTextComponent (Element el) {
    super(el);
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  @Override
  public PotentialNode<Element> createInput () {
    return newBuilder()
      .setTagName("textarea")
      ;
  }

  public static GwtInputTextComponent getInputTextComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("input-text");
      final GwtInputTextComponent component = ComponentNamespace.getComponent(e, getUi);
      return component;
  }

  public static GwtInputTextComponent create (ComponentOptions<Element, GwtInputTextComponent> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return NEW_INPUT_TEXT.constructComponent(opts, getUi);
  }

}
