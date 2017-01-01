package xapi.model.api;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;

import java.util.Map.Entry;

@JsType
public interface Model {

  //attributes
  @JsIgnore
  <T> T getProperty(String key);
  @JsIgnore
  <T> T getProperty(String key, T dflt);
  @JsIgnore
  <T> T getProperty(String key, Out1<T> dflt);

  @JsIgnore
  default <T> T getOrSaveProperty(String key, Out1<T> dflt) {
    boolean save = !hasProperty(key);
    T val = getProperty(key, dflt);
    if (save) {
      setProperty(key, val);
    }
    return val;
  }

  @JsIgnore
  default <T> T getOrCreate(Out1<T> getter, Out1<T> factory, In1<T> setter) {
    T is = getter.out1();
    if (is == null) {
      is = factory.out1();
      setter.in(is);
    }
    return is;
  }

  @JsIgnore
  default <T> T compute(Out1<T> getter, In1Out1<T, T> mapper, In1<T> setter) {
    T was = getter.out1();
    final T is = mapper.io(was);
    if (was != is) {
      setter.in(is);
    }
    return is;
  }

  @JsIgnore
  boolean hasProperty(String key);

  @JsIgnore
  Class<?> getPropertyType(String key);
  @JsIgnore
  Iterable<Entry<String, Object>> getProperties();
  @JsIgnore
  String[] getPropertyNames();
  @JsIgnore
  Model setProperty(String key, Object value);
  @JsIgnore
  Model removeProperty(String key);
  @JsIgnore
  void clear();

  @JsProperty
  ModelKey getKey();
  @JsProperty
  void setKey(ModelKey key);
  @JsProperty
  String getType();

}
