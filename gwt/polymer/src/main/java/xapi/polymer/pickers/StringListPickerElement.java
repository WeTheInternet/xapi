package xapi.polymer.pickers;

import static elemental.events.KeyboardEvent.KeyCode.ENTER;

import static xapi.components.impl.JsSupport.addClassName;
import static xapi.components.impl.JsSupport.is;
import static xapi.polymer.core.PolymerElement.newFlexVertical;
import static xapi.polymer.core.PolymerElement.newIconButtonFloatingMini;
import static xapi.polymer.core.PolymerElement.newLayoutCenterHorizontal;
import static xapi.polymer.pickers.StringPickerElement.NEW_STRING_PICKER;

import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.polymer.core.PolymerElement;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Display;
import xapi.ui.html.api.Style.Rgb;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;

import elemental.dom.Element;

@JsType
@WebComponent(tagName=StringListPickerElement.TAG_NAME)
@Css(style={
  @Style(
    names = "."+StringListPickerElement.CLASS_NAME+" > core-label > div > .polymer",
    display = Display.Block,
    width = @Unit(value=100, type=UnitType.Pct)
  ),
  @Style(
    names = ".polymer .add",
    backgroundColor = @Rgb(r=0, g=0xff, b=0)
  )
})
public interface StringListPickerElement extends
ListItemElement
{

  String TAG_NAME = "xapi-string-list-picker";
  String CLASS_NAME = "xapi-list";

  WebComponentFactory<StringListPickerElement> NEW_STRING_LIST_PICKER = GWT.create(StringListPickerElement.class);

  @JsProperty
  StringPickerElement getStringPicker();

  @JsProperty
  void setStringPicker(StringPickerElement stringPicker);

  @Override
  default void onCreated(Element element) {
    setRemovable(true);
    addClassName(element, CLASS_NAME);
    StringPickerElement stringPicker = NEW_STRING_PICKER.newComponent();
    setStringPicker(stringPicker);
    onAfterCreated(e->{
      Element core = getCoreLabel();
      Element box = newFlexVertical("five");
      box.setClassName("polymer");
      Element inputBox = newLayoutCenterHorizontal();
      inputBox.appendChild(stringPicker.getPolymer().element());
      PolymerElement addButton = newIconButtonFloatingMini("add");
      addButton.element().setClassName("add");
      inputBox.appendChild(addButton.element());
      box.appendChild(inputBox);
      box.appendChild(getPolymer().element());
      core.appendChild(box);
      addButton.onClick(this::addNewValue);
    }, true);
    stringPicker.getPolymer().onKeyUp(ev -> {
      if (ev.getKeyCode() == ENTER) {
        // Treat this as if the user clicked the add button
        addNewValue(null);
      }
    });
  }

  default void addNewValue(ClickEvent e) {
    String value = getStringPicker().getValue();
    if (is(value)) {
      addValue(value);
      getStringPicker().setValue("");
    }

  }
}
