package xapi.polymer.core;

import com.google.gwt.core.client.js.JsType;

import xapi.components.api.HasElement;
import xapi.components.api.NativelySupported;

import elemental.dom.Element;

@JsType
public interface PaperView <E extends Element, P extends PaperView<E, P>> extends HasElement<E>{

  @NativelySupported
  P setAttribute(String name, String value);

  default P setAttr(String name) {
    return setAttribute(name, "");
  }

  @NativelySupported
  String getAttribute();
}
