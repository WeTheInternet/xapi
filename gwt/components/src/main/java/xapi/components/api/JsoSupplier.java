package xapi.components.api;

import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;

public class JsoSupplier implements Supplier<Object> {

  private JavaScriptObject jso;

  public JsoSupplier(JavaScriptObject jso) {
    this.jso = jso;
  }

  @Override
  public native Object get()
  /*-{
		return this.@xapi.components.api.JsoSupplier::jso
				.call(this.__caller__ || this);
  }-*/;
}
