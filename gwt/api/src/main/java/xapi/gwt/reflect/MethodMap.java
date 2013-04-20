package xapi.gwt.reflect;

import java.lang.reflect.Method;

public abstract class MethodMap extends MemberMap{
  
  
  protected MethodMap() {}
  
  public final Method getMethod (String name, Class<?>[] signature) 
    throws NoSuchMethodException{
    String sig = name+getSignature(signature);
    Method method = getOrMakeMember(sig, this);
    if (method == null)
      throw new NoSuchMethodException(sig+" doesn't exist");
    return method;
  }

  public final Method getDeclaredMethod (String name, Class<?>[] signature) 
      throws NoSuchMethodException{
    String sig = name+getSignature(signature);
    Method method = getOrMakeMember(sig, this);
    if (method == null)
      throw new NoSuchMethodException(sig+" doesn't exist");
    return method;
  }

  public final Method[] getMethods () {
    return getMembers(this);
  }
  
  public final Method[] getDeclaredMethods () {
    return getDeclaredMembers(this);
  }
  
}
