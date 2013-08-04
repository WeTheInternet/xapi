package com.google.gwt.reflect.client;

import java.lang.reflect.Constructor;

public class ConstructorMap <T> extends MemberMap{

  protected ConstructorMap() {}

  public final Constructor<T> getConstructor(Class<?>[] signature)
    throws NoSuchMethodException{
    String sig = "__init"+JsMemberPool.getSignature(signature);
    Constructor<T> ctor = getOrMakePublicMember(sig, this);
    if (ctor == null)
      throw new NoSuchMethodException("Constructor "+sig+" not found");
    return ctor;
  }

  public final Constructor<T> getDeclaredConstructor(Class<?>[] signature)
      throws NoSuchMethodException{
    String sig = "__init"+JsMemberPool.getSignature(signature);
    Constructor<T> ctor = getOrMakeDeclaredMember(sig, this);
    if (ctor == null)
      throw new NoSuchMethodException("Constructor "+sig+" not found");
    return ctor;
  }

  @SuppressWarnings("unchecked")
  public final Constructor<T>[] getConstructors () {
    return getPublicMembers(this, new Constructor[0]);
  }

  @SuppressWarnings("unchecked")
  public final Constructor<T>[] getDeclaredConstructors() {
    return getDeclaredMembers(this, new Constructor[0]);
  }


}
