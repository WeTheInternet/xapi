package xapi.components.api;

import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.dom.Element;

public class JsoConstructorSupplier<E extends IsWebComponent<? extends Element>> implements Supplier<E> {

  private final JavaScriptObject ctor;

  public JsoConstructorSupplier(JavaScriptObject ctor) {
    this.ctor = ctor;
  }

  @Override
  public final native E get()
  /*-{
		var ctor = this.@xapi.components.api.JsoConstructorSupplier::ctor;
		return new ctor();
  }-*/;

}
