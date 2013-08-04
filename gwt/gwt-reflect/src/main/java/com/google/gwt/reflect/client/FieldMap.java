package com.google.gwt.reflect.client;

import java.lang.reflect.Field;

public class FieldMap extends MemberMap{

  protected FieldMap() {}

  public final Field getField(String name)
    throws NoSuchFieldException{
    Field field = getOrMakePublicMember(name, this);
    if (field == null) {
      throw new NoSuchFieldException("Field "+name+" not found.");
    }
    return field;
  }

  public final Field getDeclaredField(String name)
      throws NoSuchFieldException{
    Field field = getOrMakeDeclaredMember(name, this);
    if (field == null) {
      throw new NoSuchFieldException("Field "+name+" not found.");
    }
    return field;
  }

  public final Field[] getFields() {
    return getPublicMembers(this, new Field[0]);
  }

  public final Field[] getDeclaredFields () {
    return getDeclaredMembers(this, new Field[0]);
  }


}
