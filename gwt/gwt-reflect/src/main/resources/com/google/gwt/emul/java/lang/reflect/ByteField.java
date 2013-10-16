
package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/** 
 * A field representing a Byte object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class ByteField extends Field{

  public ByteField(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Byte.class, declaringClass, name, modifiers, accessor);
  }
  
//    public void setByte(Object obj, byte b)
//  throws IllegalArgumentException, IllegalAccessException {
//      set(obj, new Byte(b));
//    }

}
