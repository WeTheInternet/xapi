
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;

/** 
 * A field representing a Boolean object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class BooleanField extends Field{

  public BooleanField(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Boolean.class, declaringClass, name, modifiers, accessor);
  }
  
    public boolean getBoolean(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Boolean)o;
  }
    
    public void setBoolean(Object obj, boolean z)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Boolean(z));
    }

}
