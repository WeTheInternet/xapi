package xapi.gwt.reflect;

import java.lang.reflect.Constructor;

public class ConstructorMap <T> extends MemberMap{

  protected ConstructorMap() {}
  
  public final Constructor<T> getConstructor(Class<?>[] signature) 
    throws NoSuchMethodException{
    String sig = "__init"+getSignature(signature);
    Constructor<T> ctor = getOrMakeMember(sig, this);
    if (ctor == null)
      throw new NoSuchMethodException("Constructor "+sig+" not found");
    return ctor;
  }

  public final Constructor<T> getDeclaredConstructor(Class<?>[] signature) 
      throws NoSuchMethodException{
    String sig = "__init"+getSignature(signature);
    Constructor<T> ctor = getOrMakeDeclaredMember(sig, this);
    if (ctor == null)
      throw new NoSuchMethodException("Constructor "+sig+" not found");
    return ctor;
  }

  public final Constructor<T>[] getConstructors () {
    return getMembers(this);
  }
  
  public final Constructor<T>[] getDeclaredConstructors() {
    return getDeclaredMembers(this);
  }

  
}
