package xapi.gwt.reflect;

import java.lang.reflect.Constructor;

public class ConstructorMap <T> extends MemberMap{

  protected ConstructorMap() {
  }
  
  
  public final Constructor<T> getConstructor(Class<?>[] signature) 
    throws NoSuchMethodException{
    return getOrMakeMember("!"+getSignature(signature), this);
  }

  public final Constructor<T> getDeclaredConstructor(Class<?>[] signature) 
      throws NoSuchMethodException{
    return getOrMakeMember("!"+getSignature(signature), this);
  }

  public final Constructor<T>[] getConstructors () {
    return getMembers(this);
  }
  
  public final Constructor<T>[] getDeclaredConstructors() {
    return getMembers(this);
  }

  
}
