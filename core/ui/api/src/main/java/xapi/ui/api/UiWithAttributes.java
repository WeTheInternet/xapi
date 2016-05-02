package xapi.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.X_Fu;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public class UiWithAttributes <E extends UiElement> implements UiFeature {

  private In1Out1<String, String> getter;
  private In2<String, String> setter;
  private ClassTo<In1Out1<String, Object>> deserializers;
  private ClassTo<In1Out1<Object, String>> serializers;

  public UiWithAttributes() {
    final StringTo<String> values = X_Collect.newStringMap(String.class);
    getter = values::get;
    setter = values::put;
  }

  public UiWithAttributes(E element) {
    getter = findGetter(element);
    if (getter != null) {
      setter = findSetter(element);
    }
    if (setter == null) {
      final StringTo<String> values = X_Collect.newStringMap(String.class);
      getter = values::get;
      setter = values::put;

    }
  }

  protected In2<String, String> findSetter(E element) {
    return null;
  }

  protected In1Out1<String,String> findGetter(E element) {
    return null;
  }

  @Override
  public void initialize(UiService service) {
    deserializers = service.getDeserializers();
    serializers = service.getSerializers();
  }

  public String getAttribute(String key) {
    return getter.io(key);
  }

  public <T, Generic extends T> T getAttributeAsType(String key, Class<Generic> cls) {
    String serialized = getAttribute(key);
    if (serialized == null) {
      return null;
    }
    final In1Out1<String, Object> deserializer = deserializers.get(cls);
    if (deserializer == null) {
      throw new IllegalStateException("Cannot deserialize unregistered type " + cls + ";" +
          "\nRegistered types: " + X_Fu.reduceToString(deserializers.keys(), Class::getCanonicalName, ", ")
      );
    }
    final Object value = deserializer.io(serialized);
    assert value == null || cls.isInstance(value) : "Registered deserializer " + deserializer +" for type " + cls
        +" did not produce an instance of " + cls+";\n Instead, received an object of type " + value.getClass()+":\n"+value;
    return (T) value;
  }

  public <T, Generic extends T> void setAttributeAsType(String key, Class<Generic> cls, T value) {
    if (value == null) {
      setAttribute(key, null);
      return;
    }
    final In1Out1<Object, String> serializer = serializers.get(cls);
    if (serializer == null) {
      throw new IllegalStateException("Cannot serialize unregistered type " + cls + ";" +
          "\nRegistered types: " + X_Fu.reduceToString(serializers.keys(), Class::getCanonicalName, ", ")
      );
    }
    final String serialized = serializer.io(value);
    setAttribute(key, serialized);
  }

  public void setAttribute(String key, String value) {
    setter.in(key, value);
  }
}
