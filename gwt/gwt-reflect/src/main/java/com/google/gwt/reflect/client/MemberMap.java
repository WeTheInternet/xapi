package com.google.gwt.reflect.client;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;

public class MemberMap extends JavaScriptObject {

  protected MemberMap() {
  }

  @SuppressWarnings("rawtypes")
  @GwtScriptOnly
  public static native void setClassData(Class<?> cls, ClassMap data)
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
    var members = @com.google.gwt.reflect.client.MemberMap::newArray()();
    for (var i in map) {
      if (map.hasOwnProperty(i))
        members.push(map[i]);
    }
    return members;
  }-*/;

  private static Class<?>[] newArray() {
    return new Class<?>[0];
  }

  public static native MemberMap newInstance()
  /*-{
    return {};
  }-*/;
}
