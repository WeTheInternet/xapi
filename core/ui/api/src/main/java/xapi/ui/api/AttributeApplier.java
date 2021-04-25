package xapi.ui.api;

import xapi.string.X_String;

public interface AttributeApplier {

  default void addAttribute(String name, String value) {
      String is = getAttribute(name);
      if (is == null) {
        setAttribute(name, value);
      } else {
        setAttribute(name, concat(name, is, value));
      }
  }

  default String concat(String name, String is, String value) {
    if (is.length() == 0) {
      return value;
    }
    switch (name) {
      case "class":
        return X_String.concatIfNotContains(is, value, " ");
      default:
        return is.concat(value);
    }
  }

  void setAttribute(String name, String value);
  String getAttribute(String name);
  void removeAttribute(String name);

}
