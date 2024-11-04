package xapi.gwt.collect;

import java.util.Iterator;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

@GwtScriptOnly
public class JsArrayIterator <E> implements Iterator<E> {

  private JavaScriptObject obj;
  private int pos;

  public JsArrayIterator(JavaScriptObject obj) {
    this.obj = obj;
  }

  @Override
  public native boolean hasNext()
  /*-{
    return this.@xapi.gwt.collect.JsArrayIterator::pos
      < this.@xapi.gwt.collect.JsArrayIterator::obj.length;
  }-*/;

  @Override
  public native E next()
  /*-{
  return this.@xapi.gwt.collect.JsArrayIterator::obj[this.@xapi.gwt.collect.JsArrayIterator::pos++];
  }-*/;

  @Override
  public native void remove()
  /*-{
  this.@xapi.gwt.collect.JsArrayIterator::obj
    .splice(--this.@xapi.gwt.collect.JsArrayIterator::pos,1);
  }-*/;

}
