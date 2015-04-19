package com.google.gwt.reflect.shared;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;

import java.lang.annotation.Annotation;

public final class ReflectUtil {

  private ReflectUtil(){}

  @UnsafeNativeLong
  public static native String nativeToString(Object o)
  /*-{
    if (o == null) {
      return "";
    }
    var s = "";
    for (var i in o) {
      if (o.hasOwnProperty(i)) {
        var val = o[i];
        if (val.l != undefined && val.m != undefined && val.h != undefined) {
          val = @java.lang.Long::toString(J)(val);
        } else if (val.length) {
          var copy = val.slice(0, val.length);
          for (var v in val) {
            if (val[v].l != undefined && val[v].m != undefined && val[v].h != undefined) {
              copy[v] = @java.lang.Long::toString(J)(val[v]);
            }
          }
          val = "{"+copy+"}";
        }
        s += i.replace(/_[0-9]+_[a-z][$]$/, '') +" = " + val + ", ";
      }
    }
    return s.replace(/, $/, '');
  }-*/;

  public static String joinClasses(final String separator, final Class<?> ... vals) {
    if (GWT.isProdMode()) {
      return joinClassesJs(separator, vals);
    }
    int ind = vals.length;
    if (ind == 0)
     {
      return "";// zero elements?  zero-length string for you!
    }
    final String[] values = new String[ind];
    for(;ind-->0;){
      final Class<?> cls = vals[ind];
      if (cls != null) {
        values[ind] = cls.getCanonicalName();
      }
    }
    // all string operations use a new array, so minimize all calls possible
    final char[] sep = separator.toCharArray();

    // determine final size and normalize nulls
    int totalSize = (values.length - 1) * sep.length;// separator size
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        values[i] = "";
      } else {
        totalSize += values[i].length();
      }
    }

    // exact size; no bounds checks or resizes
    final char[] joined = new char[totalSize];
    ind = 0;
    // note, we are iterating all the elements except the last one
    int i = 0;
    final int end = values.length - 1;
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

  private static String joinClassesJs(final String separator, final Class<?> ... vals) {
    // In javascript, we are far better off to just do String += than relying on char arrays,
    // as JS has to do some nasty charCode functions to make the String(char[]) constructor work.
    final int ind = vals.length;
    String value = "";
    if (ind == 0) {
      return value;// zero elements?  zero-length string for you!
    }
    for(int i = 0;i < ind; i ++){
      final Class<?> cls = vals[ind];
      if (i > 0) {
        value += separator;
      }
      if (cls != null) {
        value += cls.getCanonicalName();
      }
    }
    return value;
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

  public static native <T> T getOrMakeMember(String key, JavaScriptObject map)
  /*-{
    return map[key] && map[key]();
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

  public static native <T> T[] getMembers(JavaScriptObject map, T[] members)
  /*-{
    for (var i in map) {
      if (map.hasOwnProperty(i))
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
