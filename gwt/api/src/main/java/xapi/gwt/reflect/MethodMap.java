package xapi.gwt.reflect;

import java.lang.reflect.Method;

public abstract class MethodMap extends MemberMap{
  
  
  protected MethodMap() {}
  
  public final Method getMethod (String name, Class<?>[] signature) 
    throws NoSuchMethodException{
    return getOrMakeMember(name+getSignature(signature), this);
  }

  public final Method getDeclaredMethod (String name, Class<?>[] signature) 
      throws NoSuchMethodException{
    return getOrMakeMember(name+getSignature(signature), this);
  }

  public final Method[] getMethods () {
    return getMembers(this);
  }
  
  public final Method[] getDeclaredMethods () {
    return getMembers(this);
  }
  
}
