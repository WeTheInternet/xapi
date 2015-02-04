package xapi.polymer.pickers;

import static xapi.components.impl.JsSupport.setAttr;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;

import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;

import elemental.dom.Element;


@JsType
@WebComponent(tagName=IntegerPickerElement.TAG_NAME)
public interface IntegerPickerElement extends
IsWebComponent<Element>,
OnWebComponentAttributeChanged,
OnWebComponentCreated<Element>,
AbstractPickerElement<Element> {

  String TAG_NAME = "xapi-int-picker";
  WebComponentFactory<IntegerPickerElement> NEW_INT_PICKER = GWT.create(IntegerPickerElement.class);

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  int getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  IntegerPickerElement setValue(int property);

  @Override
  default void onCreated(Element element) {
    initializePolymer("paper-slider");
    setAttr(getPolymer().element(), "editable");
    getPolymer().onCoreChange(e -> setValue(getPolymer().valueAsInt()));
  }

  default void setMax(int max) {
    getPolymer().element().setAttribute("max", Integer.toString(max));
  }

  default void setMin(int min) {
    getPolymer().element().setAttribute("min", Integer.toString(min));
  }

  @Override
  default void onAttributeChanged(String name, String oldVal, String newVal) {
    switch (name) {
    case "value":
      getPolymer().setValue(newVal);
      break;
    }
  }

}
