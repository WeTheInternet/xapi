package xapi.gwt.reflect;

import java.lang.reflect.Field;

public class FieldMap extends MemberMap{

  protected FieldMap() {}
  
  public final Field getField(String name) 
    throws NoSuchFieldException{
    return getOrMakeMember(name, this);
  }

  public final Field getDeclaredField(String name) 
      throws NoSuchFieldException{
    return getOrMakeMember(name, this);
  }

  public final Field[] getFields() {
    return getMembers(this);
  }
  
  public final Field[] getDeclaredFields () {
    return getMembers(this);
  }

  
}
