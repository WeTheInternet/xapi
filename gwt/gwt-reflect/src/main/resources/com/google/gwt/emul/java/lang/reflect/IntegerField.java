
package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/** 
 * A field representing a Integer object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class IntegerField extends Field{

  public IntegerField(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Integer.class, declaringClass, name, modifiers, accessor);
  }
  
    public int getInt(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Integer)o;
  }
    
    public void setInt(Object obj, int i)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Integer(i));
    }

}
