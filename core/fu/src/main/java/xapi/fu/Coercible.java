package xapi.fu;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface Coercible {

  default String listSeparator() {
    return "";
  }

  default String coerce(Object obj) {
    final String listSeparator = listSeparator();
    if (obj instanceof Iterable){
      StringBuilder b = new StringBuilder();
      Iterator i = ((Iterable)obj).iterator();
      if (i.hasNext()) {
        if (!listSeparator.isEmpty()) {
          b.append("[");
        }
        b.append(coerce(i.next()));
      }
      for (
          ;i.hasNext();) {
        final Object value = i.next();
        b.append(listSeparator);
        b.append(coerce(value));
      }
      if (!listSeparator.isEmpty()) {
        b.append("]");
      }
      return b.toString();
    } else if (obj != null && obj.getClass().isArray()){
      StringBuilder b = new StringBuilder();
      int max = X_Fu.getLength(obj);
      if (max == 0) {
        if (listSeparator.isEmpty()) {
          return "";
        }
        return "[]";
      }

      if (!listSeparator.isEmpty()) {
        b.append("[");
      }
      b.append(coerce(X_Fu.getValue(obj, 0)));
      for (int i = 1; i < max; i++ ) {
        final Object value = X_Fu.getValue(obj, i);
        b.append(listSeparator);
        b.append(coerce(value));
      }
      if (!listSeparator.isEmpty()) {
        b.append("]");
      }
      return b.toString();
    } else {
      return String.valueOf(obj);
    }
  }
}
