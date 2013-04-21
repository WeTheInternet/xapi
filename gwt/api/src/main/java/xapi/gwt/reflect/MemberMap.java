package xapi.gwt.reflect;

import com.google.gwt.core.client.JavaScriptObject;

public class MemberMap extends JavaScriptObject {

  protected MemberMap() {
  }

  public static String getSignature(Class<?>... signature) {
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < signature.length; i++) {
      key.append('_');
      if (signature[i].isPrimitive()) {
        key.append(signature[i].getName());
      } else {
        key.append(getSeedId(signature[i]));
      }
    }
    return key.toString();
  }

  private static native int getSeedId(Class<?> cls)
  /*-{
    return cls.@java.lang.Class::seedId;
  }-*/;
  
  @SuppressWarnings("rawtypes")
  public static native void setClassData(Class<?> cls, ClassMap data)
  /*-{
     cls.@java.lang.Class::classData = data;
   }-*/;

  public static native <T> T getOrMakeMember(String key, JavaScriptObject map)
  /*-{
    return map[key] && map[key]();
  }-*/;
  
  public static native <T> T getOrMakeDeclaredMember(String key, JavaScriptObject map)
  /*-{
    return map[key] && map[key].declared && map[key]();
  }-*/;

  public static native <T> T[] getMembers(JavaScriptObject map)
  /*-{
    var members = [];
    for (var i in map) {
      if (map.hasOwnProperty(i))
        members.push(map[i]());
    }
    return members;
  }-*/;
  
  public static native <T> T[] getDeclaredMembers(JavaScriptObject map)
  /*-{
    var members = [];
    for (var i in map) {
      if (map.hasOwnProperty(i) && map[i].declared)
        members.push(map[i]());
    }
    return members;
  }-*/;

  public static native MemberMap newInstance()
  /*-{
    return {};
  }-*/;
}
