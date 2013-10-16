
package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/** 
 * A field representing a Long object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class LongField extends Field{

  public LongField(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Long.class, declaringClass, name, modifiers, accessor);
  }
  
    public long getLong(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Long)o;
  }
    
    public void setLong(Object obj, long l)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Long(l));
    }

}
