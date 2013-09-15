
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;

/** 
 * A field representing a Float object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class FloatField extends Field{

  public FloatField(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Float.class, declaringClass, name, modifiers, accessor);
  }
  
    public float getFloat(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Float)o;
  }
    
    public void setFloat(Object obj, float f)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Float(f));
    }

}
