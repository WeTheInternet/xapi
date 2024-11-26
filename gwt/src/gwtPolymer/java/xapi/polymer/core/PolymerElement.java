package xapi.polymer.core;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.events.KeyboardEvent;
import elemental.js.dom.JsElement;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.HasElement;

import static xapi.components.impl.JsFunctionSupport.wrapConsumer;
import static xapi.components.impl.JsSupport.*;
import static xapi.polymer.core.PolymerSupport.setPolymerAttr;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;

import java.util.function.Consumer;

@JsType
public interface PolymerElement extends HasElement<Element> {

  @JsProperty
  <V> V value();

  default int valueAsInt() {
    return getInt(this, "value");
  }

  default double valueAsDouble() {
    return getDouble(this, "value");
  }

  default long valueAsLong() {
    return getLong(this, "value");
  }

  default boolean valueAsBoolean() {
    return getBoolean(this, "value");
  }

  void addEventListener(String name, JavaScriptObject func);

  void removeEventListener(String name, JavaScriptObject func);

  default EventRemover onCoreChange(Consumer<Event> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("core-change", func);
    return () ->  removeEventListener("core-change", func);
  }

  default EventRemover onChange(Consumer<Event> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("change", func);
    return () ->  removeEventListener("change", func);
  }

  default EventRemover onClick(Consumer<ClickEvent> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("click", func);
    return () ->  removeEventListener("click", func);
  }

  default EventRemover onKeyUp(Consumer<KeyboardEvent> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("keyup", func);
    return () ->  removeEventListener("keyup", func);
  }

  default EventRemover onKeyDown(Consumer<KeyboardEvent> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("keydown", func);
    return () ->  removeEventListener("keydown", func);
  }

  default EventRemover onKeyPress(Consumer<KeyboardEvent> event) {
    JavaScriptObject func = wrapConsumer(event);
    addEventListener("keypress", func);
    return () ->  removeEventListener("keypress", func);
  }

  @JsProperty
  void setChecked(boolean checked);

  default void attribute(String name) {
    setAttr(element(), name);
  }

  @JsProperty
  void setValue(Object newVal);

  default void setValueAsInt(int newVal) {
    setInt(this, "value", newVal);
  }

  default void setValueAsDouble(double newVal) {
    setDouble(this, "value", newVal);
  }

  default void setValueAsBoolean(boolean newVal) {
    setBoolean(this, "value", newVal);
  }

  default void setValueAsLong(long newVal) {
    setLong(this, "value", newVal);
  }

  @JsProperty
  void setLabel(String label);

  void appendChild(Element e);

  @JsProperty
  void setInnerHTML(String html);

  @Override
  default Element element() {
    return (JsElement)this;
  }

  default PolymerElement querySelector(String selector) {
    return (PolymerElement) element().querySelector(selector);
  }

  static PolymerElement newButton(String text) {
    Element icon = newElement("paper-button");
    icon.setInnerText(text);
    icon.setClassName("xapi-button");
    return (PolymerElement) icon;
  }

  static PolymerElement newButtonRaised(String text) {
    return setPolymerAttr(newButton(text), "raised");
  }

  static PolymerElement newIconButtonFloatingMini(String iconName) {
    Element icon = newIconButtonFloating(iconName);
    setAttr(icon, "mini");
    return (PolymerElement) icon;
  }

  static Element newIconButtonFloating(String iconName) {
    Element icon = newElement("paper-fab");
    icon.setAttribute("icon", iconName);
    return icon;
  }

  static PolymerElement newIconButton(String iconName) {
    Element icon = newElement("core-icon-button");
    icon.setAttribute("icon", iconName);
    return (PolymerElement) icon;
  }

  static PolymerElement newIcon(String iconName) {
    Element icon = newElement("core-icon");
    icon.setAttribute("icon", iconName);
    return (PolymerElement) icon;
  }

  static Element newFlex() {
    return setAttr(newElement("div"), "flex");
  }

  static Element newFlexHorizontal() {
    return setAttr(newFlex(), "horizontal");
  }

  static Element newFlexHorizontal(String size) {
    return setAttr(newFlexHorizontal(), size);
  }

  static Element newFlexVertical() {
    return setAttr(newFlex(), "vertical");
  }

  static Element newFlexVertical(String size) {
    return setAttr(newFlexVertical(), size);
  }

  static Element newLayout() {
    return setAttr(newElement("div"), "layout");
  }

  static Element newLayoutCenter() {
    return setAttr(newLayout(), "center");
  }

  static Element newLayoutCenterHorizontal() {
    return setAttr(newLayoutCenter(), "horizontal");
  }

  static Element newLayoutCenterVertical() {
    return setAttr(newLayoutCenter(), "vertical");
  }

  static Element newLayoutHorizontal() {
    return setAttr(newLayout(), "horizontal");
  }

  static Element newLayoutVertical() {
    return setAttr(newLayout(), "vertical");
  }

  @JsProperty
  boolean checked();

}
