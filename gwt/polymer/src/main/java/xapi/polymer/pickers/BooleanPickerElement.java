package xapi.polymer.pickers;

import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;
import xapi.polymer.core.PolymerElement;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;

@JsType
@WebComponent(tagName=BooleanPickerElement.TAG_NAME)
@Css(
  style=@Style(
    names=".xapi ::shadow #checkboxContainer",
    margin=@Unit(type=UnitType.Auto)
  )
)
public interface BooleanPickerElement extends
AbstractPickerElement<Element>,
OnWebComponentAttributeChanged,
OnWebComponentCreated<Element> {

  String TAG_NAME = "xapi-boolean-picker";
  WebComponentFactory<BooleanPickerElement> NEW_BOOLEAN_PICKER = GWT.create(BooleanPickerElement.class);

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  Boolean getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  void setValue(Boolean property);

  @Override
  default void onCreated(Element element) {
    initializePolymer("paper-checkbox");
    getPolymer().onCoreChange(e->{
      setValue(checkbox().checked());
    });
  }

  @Override
  default void onAttributeChanged(String name, String oldVal, String newVal) {
    // Used when the user manually sets <xapi-enum-picker>.value="newVal"
    // This will also be called whenever the radio group is updated via clicks,
    // however, this has no effect as the selected variable already equals newVal
    switch (name) {
    case "value":
      checkbox().setChecked("true".equals(newVal));
      break;
    }
  }

  default PolymerElement checkbox() {
    return getPolymer();
  }
}
