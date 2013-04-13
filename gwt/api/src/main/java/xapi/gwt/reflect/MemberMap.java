package xapi.gwt.reflect;

import com.google.gwt.core.client.JavaScriptObject;

public class MemberMap extends JavaScriptObject {

  protected MemberMap() {
  }

  public static String getSignature(Class<?>... signature) {
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < signature.length; i++) {
      if (signature[i].isPrimitive()) {
        key.append(signature[i].getName());
      } else {
        key.append(getSeedId(signature[i]));
      }
      key.append('-');
    }
    return key.toString();
  }

  private static native int getSeedId(Class<?> cls)
  /*-{
    return cls.@java.lang.Class::seedId;
  }-*/;
  
  public static native void setClassData(Class<?> cls, ClassMap data)
  /*-{
     cls.@java.lang.Class::classData = data;
   }-*/;

  public static native <T> T getOrMakeMember(String key, JavaScriptObject map)
  /*-{
    return map[key]();
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

  public static native MemberMap newInstance()
  /*-{
    return {};
  }-*/;
}
