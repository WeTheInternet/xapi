package xapi.dev.gwtc.api;

import xapi.fu.In1Out1;
import xapi.log.X_Log;

import java.lang.reflect.Method;

/**
 * Used to convert reflective objects, Class, Method and Package into GwtcUnit objects.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
public class GwtcReflectionConverter implements In1Out1<Object, GwtcUnit> {

  @Override
  public GwtcUnit io(Object from) {
    if (from instanceof Class) {
      return newGwtcData((Class<?>) from);
    } else if (from instanceof Method) {
      return newGwtcData((Method) from);
    } else if (from instanceof Package) {
      return newGwtcData((Package) from);
    } else {
      X_Log.warn(getClass(), "Unsupported toString object type", from == null ? "null" : from
          .getClass(), from);
    }
    return null;
  }

  protected GwtcUnit newGwtcData(Class<?> from) {
    return new GwtcUnit(from);
  }

  protected GwtcUnit newGwtcData(Package from) {
    return new GwtcUnit(from);
  }

  protected GwtcUnit newGwtcData(Method from) {
    return new GwtcUnit(from);
  }

}
