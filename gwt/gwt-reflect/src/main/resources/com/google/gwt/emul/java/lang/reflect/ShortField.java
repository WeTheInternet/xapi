
package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/** 
 * A field representing a Short object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class ShortField extends Field{

  public ShortField(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Short.class, declaringClass, name, modifiers, accessor);
  }
  
    public short getShort(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Short)o;
  }
    
    public void setShort(Object obj, short s)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Short(s));
    }

}
