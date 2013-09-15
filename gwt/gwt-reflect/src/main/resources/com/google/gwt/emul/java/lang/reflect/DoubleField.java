
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;

/** 
 * A field representing a Double object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class DoubleField extends Field{

  public DoubleField(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Double.class, declaringClass, name, modifiers, accessor);
  }
  
    public double getDouble(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Double)o;
  }
    
    public void setDouble(Object obj, double d)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Double(d));
    }

}
