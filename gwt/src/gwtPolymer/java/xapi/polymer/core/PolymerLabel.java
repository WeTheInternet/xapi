package xapi.polymer.core;

import static xapi.components.impl.JsSupport.create;
import static xapi.ui.html.api.HtmlTemplate.KEY_VALUE;

import elemental.dom.Element;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.annotation.common.Property;
import xapi.elemental.X_Elemental;
import xapi.elemental.api.PotentialNode;
import xapi.ui.html.api.El;
import xapi.ui.html.api.NoUi;
import xapi.util.api.ConvertsValue;

@El(
    tag = "core-label",
    properties = {
      @Property(name="layout", value=""),
      @Property(name="horizontal", value=""),
      @Property(name="center", value=""),
    },
    html="$children"
)
@JsType
public interface PolymerLabel {

  ConvertsValue<PolymerLabel, PotentialNode<Element>> BUILDER = X_Elemental.<PolymerLabel, Element>toElementBuilder(PolymerLabel.class);

  @El(
    html="$value",
    className="label",
    properties={
      @Property(name="flex"),
      @Property(name="three"),
      @Property(name="self-center"),
    })
  @JsProperty
  String label();

  @El(
    tag="",
    html=KEY_VALUE,
    useToHtml=PolymerInput.class
  )
  @JsProperty
  PolymerInput input();

  @NoUi
  @JsProperty
  PolymerLabel input(PolymerInput input);

  @NoUi
  @JsProperty
  PolymerLabel label(String label);

  @NoUi
  default Element build() {
    PotentialNode<Element> node = BUILDER.convert(this);
    return node.getElement();
  }

  @NoUi
  public static PolymerLabel newLabel() {
    return create();
  }

}
