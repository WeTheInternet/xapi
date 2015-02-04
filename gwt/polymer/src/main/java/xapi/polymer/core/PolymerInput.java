package xapi.polymer.core;

import static xapi.components.impl.JsSupport.create;

import xapi.annotation.common.Property;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

@JsType
@Html(
  body=@El(
    tag="#tagName",
    className="polymer",
    properties= {
      @Property(name="flex"),
      @Property(name="five"),
      @Property(name="for"),
    }
    )
)
public interface PolymerInput {

  @JsProperty
  String tagName();

  @JsProperty
  PolymerInput tagName(String tagName);

  public static PolymerInput newInput() {
    return create();
  }
}
