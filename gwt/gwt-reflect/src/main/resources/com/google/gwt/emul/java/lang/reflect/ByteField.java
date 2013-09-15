
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;

/** 
 * A field representing a Byte object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class ByteField extends Field{

  public ByteField(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Byte.class, declaringClass, name, modifiers, accessor);
  }
  
//    public void setByte(Object obj, byte b)
//  throws IllegalArgumentException, IllegalAccessException {
//      set(obj, new Byte(b));
//    }

}
