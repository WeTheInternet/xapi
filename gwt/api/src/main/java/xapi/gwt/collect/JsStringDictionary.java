package xapi.gwt.collect;

import xapi.collect.api.StringDictionary;
import xapi.util.api.ReceivesValue;

public class JsStringDictionary <V> implements StringDictionary<V>{

  protected JsStringDictionary() {
  }

  public static native <V> JsStringDictionary<V> create()
  /*-{
    return {};
  }-*/;


  @Override
  public final native V getValue(String key)
  /*-{
    return this[key];
  }-*/;

  @Override
  public final native boolean hasKey(String key)
  /*-{
    return this[key] != undefined;
  }-*/;

  @Override
  public final native V setValue(String key, V value)
  /*-{
    var r = this[key];
    this[key] = value;
    return r;
  }-*/;

  @Override
  public final native V removeValue(String key)
  /*-{
    var r = this[key];
    delete this[key];
    return r;
  }-*/;

  @Override
  public final native void clearValues()
  /*-{
     for (var i in this) {
       if (Object.prototype.hasOwnProperty.apply(this, i))
         delete this[i];
     }
  }-*/;

  @Override
  public final native void forKeys(ReceivesValue<String> receiver)
  /*-{
    for (var i in this) {
      if (Object.prototype.hasOwnProperty.apply(this, i))
        receiver.@xapi.util.api.ReceivesValue::set(Ljava/lang/Object;)(i);
    }
  }-*/;

}
