package xapi.polymer.pickers;

import elemental.dom.Element;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttached;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;
import xapi.polymer.core.PolymerElement;

import com.google.gwt.core.client.GWT;


@JsType
@WebComponent(tagName=StringPickerElement.TAG_NAME)
public interface StringPickerElement extends
IsWebComponent<Element>,
OnWebComponentAttributeChanged<Element>,
OnWebComponentCreated<Element>,
OnWebComponentAttached<Element>,
AbstractPickerElement<Element> {
  String TAG_NAME = "xapi-string-picker";
  WebComponentFactory<StringPickerElement> NEW_STRING_PICKER = GWT.create(StringPickerElement.class);

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  String getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  StringPickerElement setValue(String property);

  @Override
  default void onCreated(Element element) {
    initializePolymer("paper-input");
    PolymerElement textField = getPolymer();
    setTextField(textField.element());
    textField.attribute("floatingLabel");
    setInstructionsElement(textField.element());
    textField.onChange(e -> setValue(textField.value()));
  }

  default void setLabel(String label) {
    getPolymer().setLabel(label);
  }

  @JsProperty
  void setTextField(Element selector);

  @JsProperty
  Element getTextField();

  @Override
  default void onAttributeChanged(Element e, String name, String oldVal, String newVal) {
    switch (name) {
    case "value":
      getTextField().setAttribute("value", newVal);
      break;
    }
  }

  @Override
  default void onAttached(Element element) {
    getTextField().getStyle().removeProperty("display");
  }

}
