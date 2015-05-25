package xapi.gwt.collect;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.Iterator;
import java.util.Map.Entry;

import xapi.annotation.inject.InstanceOverride;
import xapi.collect.api.StringTo;
import xapi.collect.impl.ArrayIterable;
import xapi.collect.impl.EntryValueAdapter;
import xapi.collect.impl.IteratorWrapper;
import xapi.platform.GwtPlatform;

@InstanceOverride(implFor=StringTo.class)
@SuppressWarnings("serial")
@GwtScriptOnly
@GwtPlatform
public class StringToGwt <V> extends JavaScriptObject implements StringTo<V>{

  protected StringToGwt() {
  }

  public static StringTo<Object> newInstance() {
    return create(Object.class);
  }

  public static native <V> StringTo<V> create(Class<? extends V> valueType)
  /*-{
   return {_v$: valueType, _k: @xapi.gwt.collect.StringToGwt::newStringArray()() };
  }-*/;

  public static native StringToGwt<Object> create()
  /*-{
   return {_k: @xapi.gwt.collect.StringToGwt::newStringArray()() };
  }-*/;

  public static native <T> StringToGwt<T> fromJso(JavaScriptObject o)
  /*-{
     var m = @xapi.gwt.collect.StringToGwt::create()();
     for (var i in o) {
       var slot = {i : this._k.length, v: o[i]};
       m._k[slot.i] = i;
       m[i] = slot;
     }
   }-*/;


  private static String[] newStringArray() {
    return new String[0];
  }


  class KeyItr extends ArrayIterable<String>{
    public KeyItr() {
      super(keyArray());
    }
    @Override
    protected void remove(final String key) {
      StringToGwt.this.remove(key);
    }
  }

  class EntryItr implements Iterator<Entry<String, V>> {

    int pos = 0;
    String[] keys = keyArray();
    int max = keys.length;
    Entry<String, V> entry;

    @Override
    public boolean hasNext() {
      return pos < max;
    }

    @Override
    public Entry<String,V> next() {
      final String key = keys[pos++];
      final V next = get(key);
      entry = new Entry<String,V>() {

        @Override
        public String getKey() {
          return key;
        }

        @Override
        public V getValue() {
          return next;
        }

        @Override
        public V setValue(final V value) {
          if (value == null) {
            return StringToGwt.this.remove(key);
          } else {
            return StringToGwt.this.put(key, value);
          }
        }
      };
      return entry;
    }

    @Override
    public void remove() {
      assert entry != null : "You must call next() before remove() in StringToGwt.entries()";
      StringToGwt.this.remove(entry.getKey());
    }

  }

  public final native JavaScriptObject toJso()
  /*-{
    var out = {}, i = this._k.length;
    while(i --> 0) {
      var k = this._k[i];
      out[k] = this[k].v;
    }
    return out;
  }-*/;


  @Override
  public final native String[] keyArray()
  /*-{
   return @com.google.gwt.lang.Array::clone([Ljava/lang/Object;)(this._k);
   }-*/;

  private final native String[] nativeKeys()
  /*-{
   return this._k;
   }-*/;

  @Override
  public final native boolean containsKey(Object key)
  /*-{
    return this.hasOwnProperty(key)&&this[key] != undefined;
  }-*/;

  @Override
  public final boolean containsValue(final Object value) {
    final String[] keys = nativeKeys();
    int i = keys.length;

    if (value == null) {
      for (;i-->0;) {
        if (get(keys[i]) == null) {
          return true;
        }
      }
    } else {
      for (;i-->0;) {
        if (value.equals(get(keys[i]))) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public final native V get(String key)
  /*-{
    return this.hasOwnProperty(key) ? this[key].v : null;
  }-*/;

  @Override
  @GwtScriptOnly
  public final native V put(String key, V value)
  /*-{
    var index = this._k.indexOf(key);
    if (index == -1) {
      var slot = {i : this._k.length, v: value};
      this[key] = slot;
      this._k[slot.i] = key;
      return null;
    }
    var slot = this[this._k[index]], v = slot.v;
    slot.v = value;
    return v;
  }-*/;

  @Override
  public final native boolean isEmpty()
  /*-{
    return this._k.length == 0;
  }-*/;

  @Override
  public final native void clear()
  /*-{
    for (
      var i = this._k.length;
      i -->0;
      delete this[this._k[i]]
    );
    this._k.length = 0;
  }-*/;

  @Override
  public final native V remove(String key)
  /*-{
    var index = this._k.indexOf(key);
    if (index == -1) return null;
    var value = this[key];
    delete this[key];
    this._k.splice(index, 1);
    return value.v;
  }-*/;

  @Override
  public final void putAll(final Iterable<Entry<String,V>> items) {
    for (final Entry<String, V> item : items) {
      put(item.getKey(), item.getValue());
    }
  }

  @Override
  public final void removeAll(final Iterable<String> items) {
    for (final String item : items) {
      remove(item);
    }
  }

  @Override
  public final Iterable<String> keys() {
    return new KeyItr();
  }

  @Override
  public final Iterable<V> values() {
    return new EntryValueAdapter<String, V>(entries());
  }

  @Override
  public final Iterable<Entry<String,V>> entries() {
    return new IteratorWrapper<Entry<String, V>>(new EntryItr());
  }

  @Override
  public final int size() {
    return nativeKeys().length;
  }

}
