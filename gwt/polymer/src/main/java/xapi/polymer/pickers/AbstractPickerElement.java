package xapi.polymer.pickers;

import static xapi.components.impl.JsSupport.addClassName;
import static xapi.components.impl.JsSupport.hideIfEmpty;
import static xapi.components.impl.JsSupport.newElement;
import static xapi.polymer.core.PolymerInput.newInput;
import static xapi.polymer.core.PolymerLabel.newLabel;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttached;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.impl.WebComponentWithCallbacks;
import xapi.polymer.core.PolymerElement;
import xapi.polymer.core.PolymerInput;
import xapi.polymer.core.PolymerLabel;

@JsType
public interface AbstractPickerElement <E extends Element> extends
IsWebComponent<E>,
OnWebComponentCreated<E>,
OnWebComponentAttached<E>,
WebComponentWithCallbacks<E> {

  String pickerFieldTag();

  default String getTitle() {
    return getTitleElement().getInnerText();
  }

  default void setTitle(String title) {
    getTitleElement().setInnerText(title);
    hideIfEmpty(getTitleElement());
  }

  @JsProperty
  Element getTitleElement();

  @JsProperty
  void setTitleElement(Element title);

  default String getInstructions() {
    return getInstructionsElement().getInnerText();
  }

  default void setInstructions(String instructions) {
    getInstructionsElement().setInnerText(instructions);
    hideIfEmpty(getInstructionsElement());
  }

  @JsProperty
  Element getInstructionsElement();

  @JsProperty
  void setInstructionsElement(Element instructions);

  @JsProperty
  Element getLabelContainer();

  @JsProperty
  void setLabelContainer(Element e);

  @Override
  default void onCreated(Element element) {
    addClassName(element, "xapi");
    Element container = getLabelContainer();
    if (container == null) {
      container = attachRoot();
    }
    Element title = newElement("h3");
    container.appendChild(title);
    setTitleElement(title);

    Element instructions = newElement("div");
    container.appendChild(instructions);
    setInstructionsElement(instructions);

    if (container != attachRoot() && container.getParentElement() == null) {
      attachRoot().appendChild(container);
    }

  }


  @Override
  default void onAttached(E element) {
    hideIfEmpty(getTitleElement());
    hideIfEmpty(getInstructionsElement());
  }

  default Element initializePolymer(String inputTag) {
    PolymerInput input = newInput().tagName(inputTag);
    PolymerLabel label = newLabel().input(input);
    Element el = label.build();
    attachRoot().appendChild(el);
    setLabelContainer(el.querySelector(".label"));
    setPolymer((PolymerElement)el.querySelector(inputTag));
    setCoreLabel(el);
    return el;
  }

  @JsProperty
  Element getShadowRoot();

  @JsProperty
  void setShadowRoot(Element e);

  default Element attachRoot() {
    // disabling shadow root for now as it doesn't really add any value...
//    Element shadow = getShadowRoot();
//    if (shadow == null) {
//      shadow = createShadowRoot(element());
//      setShadowRoot(shadow);
//    }
//    return shadow;
    return element();
  }

  @JsProperty
  Element getCoreLabel();

  @JsProperty
  void setCoreLabel(Element coreLabel);

  @JsProperty
  PolymerElement getPolymer();

  @JsProperty
  void setPolymer(PolymerElement element);

}
