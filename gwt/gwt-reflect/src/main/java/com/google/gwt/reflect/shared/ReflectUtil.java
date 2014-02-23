package com.google.gwt.reflect.shared;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

public final class ReflectUtil {

  private ReflectUtil(){}
  
  public static String joinClasses(String separator, Class<?> ... vals) {
    int ind = vals.length;
    if (ind == 0) return "";// zero elements?  zero-length string for you!
    final String[] values = new String[ind];
    for(;ind-->0;){
      Class<?> cls = vals[ind];
      if (cls != null)
        values[ind] = cls.getCanonicalName();
    }
    // all string operations use a new array, so minimize all calls possible
    final char[] sep = separator.toCharArray();
  
    // determine final size and normalize nulls
    int totalSize = (values.length - 1) * sep.length;// separator size
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null)
        values[i] = "";
      else
        totalSize += values[i].length();
    }
  
    // exact size; no bounds checks or resizes
    final char[] joined = new char[totalSize];
    ind = 0;
    // note, we are iterating all the elements except the last one
    int i = 0, end = values.length - 1;
    for (; i < end; i++) {
      System.arraycopy(values[i].toCharArray(), 0, joined, ind, values[i].length());
      ind += values[i].length();
      System.arraycopy(sep, 0, joined, ind, sep.length);
      ind += sep.length;
    }
    // now, add the last element;
    // this is why we checked values.length == 0 off the hop
    final String last = values[end];
    System.arraycopy(last.toCharArray(), 0, joined, ind, last.length());
  
    return new String(joined);
  }

  @GwtScriptOnly
  public static native void setClassData(Class<?> cls, Object data)
  /*-{
     cls.@java.lang.Class::classData = data;
   }-*/;

  public static native boolean hasMember(String key, JavaScriptObject map)
  /*-{
    return map[key] !== undefined;
  }-*/;

  public static native <T> T getOrMakePublicMember(String key, JavaScriptObject map)
  /*-{
    return map[key] && map[key].pub && map[key]();
  }-*/;

  public static native <T> T getOrMakeDeclaredMember(String key, JavaScriptObject map)
  /*-{
    return map[key] && map[key].declared && map[key]();
  }-*/;

  public static native <T> T[] getPublicMembers(JavaScriptObject map, T[] members)
  /*-{
    for (var i in map) {
      if (map.hasOwnProperty(i) && map[i].pub)
        members.push(map[i]());
    }
    return members;
  }-*/;

  public static native <T> T[] getDeclaredMembers(JavaScriptObject map, T[] members)
  /*-{
    for (var i in map) {
      if (map.hasOwnProperty(i) && map[i].declared)
        members.push(map[i]());
    }
    return members;
  }-*/;

  public static native Annotation[] getAnnotations(JavaScriptObject map, Annotation[] members)
  /*-{
    if (map.annos)
      map = map.annos();
    for (var i in map) {
      if (map.hasOwnProperty(i))
        members.push(map[i]);
    }
    return members;
  }-*/;

  public static native <T extends Annotation> T getAnnotation(JavaScriptObject map, Class<T> cls)
  /*-{
    if (map.annos)
      map = map.annos();
    return map[cls.@java.lang.Class::getName()()];
  }-*/;

  public static native Class<?>[] getRawClasses(JavaScriptObject map)
  /*-{
    var members = @com.google.gwt.reflect.shared.ReflectUtil::newArray()();
    for (var i in map) {
      if (map.hasOwnProperty(i))
        members.push(map[i]);
    }
    return members;
  }-*/;

  private static Class<?>[] newArray() {
    return new Class<?>[0];
  }

}
