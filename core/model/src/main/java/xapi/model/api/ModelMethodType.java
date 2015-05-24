/**
 *
 */
package xapi.model.api;

import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.SetterFor;
import xapi.model.impl.ModelNameUtil;


/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public enum ModelMethodType {

  GET("get"), /*GET_WITH_DEFAULT,*/ /*CHECK,*/
  SET("set"), /*CHECK_AND_SET,*/ ADD("add"), ADD_ALL("addAll"),
  REMOVE("remove"), CLEAR("clear");

  private String prefix;
  private ModelMethodType(final String prefix) {
    this.prefix = prefix;
  }

  public static String deducePropertyName(final String name,
      final GetterFor getter, final SetterFor setter, final DeleterFor deleter) {
    switch (deduceMethodType(name, getter, setter, deleter)) {
      case GET:
        return getter == null || getter.value().isEmpty() ? ModelNameUtil.stripGetter(name)
            : getter.value();
      case SET:
      case ADD:
      case ADD_ALL:
        return setter == null || setter.value().isEmpty() ? ModelNameUtil.stripSetter(name)
            : setter.value();
      case REMOVE:
      case CLEAR:
        return deleter == null || deleter.value().isEmpty() ? ModelNameUtil.stripRemover(name)
            : deleter.value();
    }
    throw new UnsupportedOperationException("Method "+name +" is not a valid model method name");
  }
  public static ModelMethodType deduceMethodType(final String name,
      final GetterFor getter, final SetterFor setter, final DeleterFor deleter) {
    if (getter != null) {
      return GET;
    }
    if (setter != null) {
      return SET;
    }
    if (deleter != null) {
      return REMOVE;
    }

    // No annotations.  We are stuck guessing based on method name. Ew.
    final String prefix = name.replaceFirst("[A-Z].*", "");
    switch(prefix) {
      case "get":
      case "is":
      case "has":
        return GET;
      case "set":
        return SET;
      case "add":
        return name.startsWith(prefix+"All") ? ADD_ALL : ADD;
      case "rem":
      case "remove":
        return name.startsWith(prefix+"All") ? CLEAR : REMOVE;
      case "clear":
        return CLEAR;
    }
    throw new UnsupportedOperationException("Method "+name +" is not a valid model method name");
  }

  public boolean isDefaultName(final String methodName, final String propertyName) {
    return methodName.equals(getDefaultName(propertyName));
  }

  public String getDefaultName(final String propertyName) {
    return prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
  }

}
