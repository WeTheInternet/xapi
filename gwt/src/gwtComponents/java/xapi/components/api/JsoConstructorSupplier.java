package xapi.components.api;

import elemental.dom.Element;
import xapi.fu.Out1;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Supplier;

public class JsoConstructorSupplier<E extends IsWebComponent<? extends Element>> implements Supplier<E>, Out1<E> {

  private final JavaScriptObject ctor;

  public JsoConstructorSupplier(JavaScriptObject ctor) {
    this.ctor = ctor;
  }

  @Override
  public final E out1() {
    return get();
  }

  @Override
  public final native E get()
  /*-{
		var ctor = this.@xapi.components.api.JsoConstructorSupplier::ctor;
		return new ctor();
  }-*/;

}
