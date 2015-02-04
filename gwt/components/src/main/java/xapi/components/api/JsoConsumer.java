package xapi.components.api;

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;

public class JsoConsumer implements Consumer<Object> {

  private JavaScriptObject obj;

  public JsoConsumer(JavaScriptObject obj) {
    this.obj = obj;
  }

  @Override
  public native void accept(Object t)
  /*-{
		this.@xapi.components.api.JsoConsumer::obj.call(
				this.__caller__ || this, t);
  }-*/;

}
