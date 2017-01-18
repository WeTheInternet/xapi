package xapi.components.api;

import xapi.fu.Out1;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Supplier;

public class JsoSupplier implements Out1<Object>, Supplier<Object> {

  private JavaScriptObject jso;

  public JsoSupplier(JavaScriptObject jso) {
    this.jso = jso;
  }

  @Override
  public final Object out1() {
    return get();
  }

  @Override
  public native Object get()
  /*-{
		return this.@xapi.components.api.JsoSupplier::jso
				.call(this.__caller__ || this);
  }-*/;
}
