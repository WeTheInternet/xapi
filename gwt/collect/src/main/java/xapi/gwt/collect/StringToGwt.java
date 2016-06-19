package xapi.gwt.collect;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;
import xapi.annotation.inject.InstanceOverride;
import xapi.collect.api.StringTo;
import xapi.collect.impl.ArrayIterable;
import xapi.collect.impl.EntryValueAdapter;
import xapi.collect.impl.IteratorWrapper;
import xapi.fu.Out2;
import xapi.platform.GwtPlatform;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * This is an old-school jso-style implementation of an insertion ordered map that is optimized for gwt.
 * TODO: make a simpler version that does not preserve order and use it when we don't care to pay for the extra abstraction.
 */
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
     if (o.hasOwnProperty("_v$") && o.hasOwnProperty("_k")) {
       return o; // TODO: perhaps make a copy?
     }
     var m = @xapi.gwt.collect.StringToGwt::create()();
     for (var i in o) {
       if (o.hasOwnProperty(i)) {
         var slot = {i : this._k.length, v: o[i]};
         m._k[slot.i] = i;
         m[i] = slot;
       }
     }
   }-*/;


  private static String[] newStringArray() {
    return new String[0];
  }


  static class KeyItr extends ArrayIterable<String>{

    private final StringToGwt src;

    public KeyItr(StringToGwt from) {
      super(from.keyArray());
      this.src = from;
    }

    @Override
    protected void remove(final String key) {
      src.remove(key);
    }
  }

  static class EntryItr <V> implements Iterator<Entry<String, V>> {

    final StringToGwt<V> src;

    int pos = 0;
    String[] keys;
    int max;
    Entry<String, V> entry;

    EntryItr(StringToGwt<V> src) {
      this.src = src;
      keys = src.keyArray();
      max = keys.length;
    }

    @Override
    public boolean hasNext() {
      return pos < max;
    }

    @Override
    public Entry<String,V> next() {
      final String key = keys[pos++];
      final V next = src.get(key);
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
            return src.remove(key);
          } else {
            return src.put(key, value);
          }
        }
      };
      return entry;
    }

    @Override
    public void remove() {
      assert entry != null : "You must call next() before remove() in StringToGwt.entries()";
      src.remove(entry.getKey());
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
  public final void addAll(final Iterable<Out2<String,V>> items) {
    for (final Out2<String, V> item : items) {
      put(item.out1(), item.out2());
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
    return new KeyItr(this);
  }

  @Override
  public final Iterable<V> values() {
    return new EntryValueAdapter<String, V>(entries());
  }

  @Override
  public final Class<String> keyType() {
    return String.class;
  }

  @Override
  public final native Class<V> valueType()
  /*-{
    return this._v$;
  }-*/;

  @Override
  public final Iterable<Entry<String,V>> entries() {
    return new IteratorWrapper<Entry<String, V>>(new EntryItr(this));
  }

  @Override
  public final int size() {
    return nativeKeys().length;
  }

}
