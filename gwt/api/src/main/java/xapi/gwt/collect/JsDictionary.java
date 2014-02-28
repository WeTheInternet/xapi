package xapi.gwt.collect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import xapi.collect.impl.ArrayIterable;
import xapi.collect.impl.EntryValueAdapter;
import xapi.collect.impl.IteratorWrapper;

import com.google.gwt.core.client.JavaScriptObject;

public class JsDictionary <V> extends JavaScriptObject {

  protected JsDictionary() {
  }

  class KeyItr extends ArrayIterable<String>{
    public KeyItr() {
      super(keyArray());
    }
    @Override
    protected void remove(String key) {
      JsDictionary.this.remove(key);
    }
  }

  class EntryItr implements Iterator<Entry<String, V>> {

    int pos = 0;
    int max = keyArray().length;
    Entry<String, V> entry;

    @Override
    public boolean hasNext() {
      return pos < max;
    }

    @Override
    public Entry<String,V> next() {
      final String key = keyArray()[pos++];
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
        public V setValue(V value) {
          if (value == null) {
            return JsDictionary.this.removeAndReturn(key);
          }
          else
            return JsDictionary.this.put(key, value);
        }
      };
      return entry;
    }

    @Override
    public void remove() {
      assert entry != null : "You must call next() before remove() in JsDictionary.entries()";
      JsDictionary.this.remove(entry.getKey());
    }

  }

  public static native <V> JsDictionary<V> create(Class<V> valueType)
  /*-{
   return {_v$: valueType, _k: @xapi.gwt.collect.JsDictionary::newStringArray()() };
  }-*/;

  private static String[] newStringArray() {
    return new String[0];
  }

  protected final native String[] keyArray()
  /*-{
   return this._k;
   }-*/;

  public final native boolean containsKey(String key)
  /*-{
    return this.hasOwnProperty(key)&&this[key] != undefined;
  }-*/;

  public final boolean containsValue(V value) {
    String[] keys = keyArray();
    int i = keys.length;

    if (value == null) {
      for (;i-->0;) {
        if (get(keys[i]) == null)
         return true;
      }
    } else {
      for (;i-->0;) {
        if (value.equals(get(keys[i])))
          return true;
      }
    }
    return false;
  }

  public final native V get(String key)
  /*-{
    return this.hasOwnProperty(key) && this[key].v;
  }-*/;

  public final native V put(String key, V value)
  /*-{
    if (this.hasOwnProperty(key)) {
      var slot = this._k[this._k.length], v = slot.v;
      slot.v = value;
      return v;
    }
    var slot = {i : this._k.length, v: value};
    this[key] = slot;
    this._k[slot.i] = key;
    return null;
  }-*/;

  public final native boolean isEmpty()
  /*-{
    return this._k.length == 0;
  }-*/;

  public final native void clear()
  /*-{
    for (
      var i = this._k.length;
      i -->0;
      delete this[this._k[i]]
    );
    this._k.length = 0;
  }-*/;

  public final native boolean remove(String key)
  /*-{
    if (this.hasOwnProperty(key)) {
      var index = this._k.indexOf(key);
      delete this[key];
      this._k.splice(index, 1);
      return true;
    }
    // we don't allow you to delete items which aren't keys... like __prototype
    return false;
  }-*/;

  public final native V removeAndReturn(String key)
  /*-{
    if (this.hasOwnProperty(key)) {
      var value = this[key];
      delete this[key];
      return value;
    }
    return null;
  }-*/;

  public final void putAll(Iterable<Entry<String,V>> items) {
    for (Entry<String, V> item : items)
      put(item.getKey(), item.getValue());
  }

  public final void removeAll(Iterable<String> items) {
    for (String item : items) {
      remove(item);
    }
  }

  public final Iterable<String> keys() {
    return new KeyItr();
  }

  public final Iterable<V> values() {
    return new EntryValueAdapter<String, V>(entries());
  }

  public final Iterable<Entry<String,V>> entries() {
    return new IteratorWrapper<Entry<String, V>>(new EntryItr());
  }

  public final int size() {
    return keyArray().length;
  }

  public final V[] toArray() {
//    String[] keys = keyArray();
//    
//    V[] array = GwtReflect.newArray(valueType(), keys.length);
//    for (int i = keys.length; i --> 0; array[i] = get(keys[i]));
//    return array;
    throw new UnsupportedOperationException();
  }

  public final Collection<V> toCollection(Collection<V> into) {
    if (into == null) {
      into = new ArrayList<V>();
    }
    String[] keys = keyArray();
    for (int i = keys.length; i-->0;into.add(get(keys[i])));
    return into;
  }

  public final Map<String,V> toMap(Map<String,V> into) {
    if (into == null) {
      into = new LinkedHashMap<String, V>();
    }
    for (Entry<String,V> entry : entries()) {
      into.put(entry.getKey(), entry.getValue());
    }
    return into;
  }

  public final native Class<V> valueType()
  /*-{
    return this._v$;
  }-*/;

  public final Class<String> keyType() {
    return String.class;
  }


}
