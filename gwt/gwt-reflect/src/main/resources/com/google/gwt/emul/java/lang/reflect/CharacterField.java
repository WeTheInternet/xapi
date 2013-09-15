
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;

/** 
 * A field representing a Character object.
 * 
 * @author "james@wetheinter.net"
 *
 */
public class CharacterField extends Field{

  public CharacterField(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(Character.class, declaringClass, name, modifiers, accessor);
  }
  
    public char getChar(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    Object o = get(obj);
    maybeThrowNullGet(o, true);
    return (Character)o;
  }
    
    public void setChar(Object obj, char c)
  throws IllegalArgumentException, IllegalAccessException {
      set(obj, new Character(c));
    }

}
