package xapi.gwt.reflect;

import java.lang.reflect.Field;

public class FieldMap extends MemberMap{

  protected FieldMap() {}
  
  public final Field getField(String name) 
    throws NoSuchFieldException{
    Field field = getOrMakeMember(name, this);
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
    return getMembers(this);
  }
  
  public final Field[] getDeclaredFields () {
    return getDeclaredMembers(this);
  }

  
}
