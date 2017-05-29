package xapi.ui.edit;

import static xapi.components.impl.WebComponentBuilder.htmlElementClass;


import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.StyleElement;

import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.fu.In1Out1;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.html.api.GwtStyles;

public class GwtInputTextComponent extends BaseInputTextComponent<Node, Element, PotentialNode<Element>> {

  public static void assemble (UiConfig<Element, StyleElement, ? extends GwtStyles, ElementalService> assembler) {
    WebComponentBuilder component = new WebComponentBuilder(htmlElementClass(), WebComponentVersion.V1);

    component.setClassName("InputText");
    ComponentOptions<Node, Element, InputTextComponent<Node, Element>> opts = new ComponentOptions<>();
    getUi = WebComponentSupport.installFactory(component, GwtInputTextComponent::new, opts);
    component.createdCallback(e->{
      final GwtInputTextComponent c = getInputTextComponent(e);
      final Element child = c.toDom().getElement();
      e.appendChild(child);
    });
    component.attachedCallback(e->{
      final GwtInputTextComponent c = getInputTextComponent(e);
      if (c.getModel() == null) {
        final Object modelId = e.getDataset().at("modelId");
        if (modelId != null) {
          final Model model = X_Model.cache().getModel(String.valueOf(modelId));
          c.setModel((ModelInputText)model);
        }
      }
    });
    NEW_INPUT_TEXT = WebComponentSupport.define(
      "input-text", component);
  }

  private static ComponentConstructor<Node, Element, InputTextComponent<Node, Element>> NEW_INPUT_TEXT;

  private static In1Out1<Element, InputTextComponent<Node, Element>> getUi;

  public GwtInputTextComponent (Element el) {
    super(el);
  }

  @Override
  protected ModelInputText createModel() {
    return null;
  }

  @Override
  public String getModelType() {
    return null;
  }

  @Override
  public String getModelId(Element element) {
    return null;
  }

  public PotentialNode<Element> newBuilder () {
    return new PotentialNode<Element>();
  }

  @Override
  public PotentialNode<Element> createInput () {
    return newBuilder()
      .setTagName("input");
  }

  public static GwtInputTextComponent getInputTextComponent (Element e) {
      assert e != null;
      assert e.getTagName().toLowerCase().equals("input-text");
      final InputTextComponent component = ComponentNamespace.getComponent(e, getUi);
      return (GwtInputTextComponent) component;
  }

  public static GwtInputTextComponent create (ComponentOptions<Node, Element, InputTextComponent<Node, Element>> opts) {
    if (opts == null) {
      opts = new ComponentOptions<>();
    }
    return (GwtInputTextComponent)NEW_INPUT_TEXT.constructComponent(opts, getUi);
  }

}
