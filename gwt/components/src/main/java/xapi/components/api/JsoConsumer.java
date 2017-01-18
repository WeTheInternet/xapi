package xapi.components.api;

import xapi.fu.In1;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Consumer;

public class JsoConsumer implements Consumer<Object>, In1<Object> {

  private JavaScriptObject obj;

  public JsoConsumer(JavaScriptObject obj) {
    this.obj = obj;
  }

  @Override
  public final void in(Object in) {
    accept(in);
  }

  @Override
  public native void accept(Object t)
  /*-{
		this.@xapi.components.api.JsoConsumer::obj.call(
				this.__caller__ || this, t);
  }-*/;

}
