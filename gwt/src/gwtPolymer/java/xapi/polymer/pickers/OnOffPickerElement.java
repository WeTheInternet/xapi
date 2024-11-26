package xapi.polymer.pickers;

import elemental.dom.Element;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;

import com.google.gwt.core.shared.GWT;

/**
 * For elements that have boolean on/off state, but also wish to handle the null or "no choice made state",
 * it is preferable to use an {@link EnumPickerElement} with the {@link OnOff} enum type.
 *
 * @author James X Nelson (james@wetheinter.net)
 *
 */
@JsType
@WebComponent(tagName="xapi-on-off-picker")
public interface OnOffPickerElement extends
EnumPickerElement<OnOffPickerElement.OnOff>,
OnWebComponentCreated<Element> {

  WebComponentFactory<OnOffPickerElement> NEW_ON_OFF_PICKER = GWT.create(OnOffPickerElement.class);

  static enum OnOff {
    ON, OFF
  }

  @Override
  default void onCreated(Element e) {
    onAfterCreated(ev-> render(null, OnOff.values()) , true);
  }

  default boolean on() {
    return getValue() == OnOff.ON;
  }

  default boolean off() {
    return getValue() == OnOff.OFF;
  }

  default boolean hasValue() {
    return getValue() != null;
  }

  default void setOn() {
    setValue(OnOff.ON);
  }

  default void setOff() {
    setValue(OnOff.OFF);
  }

  @JsProperty
  String getOnString();

  @JsProperty
  void setOnString(String onString);

  @JsProperty
  String getOffString();

  @JsProperty
  void setOffString(String onString);

  @JsProperty
  String getNullString();

  @JsProperty
  void setNullString(String onString);

  default String stringValue() {
    OnOff value = getValue();
    if (value == null) {

    }
    if (value == OnOff.ON)
      return getOnString();
    else
      return getOffString();
  }

}
