package xapi.ui.api;

public interface AttributeApplier {

  void addAttribute(String name, String value);
  void setAttribute(String name, String value);
  String getAttribute(String name);
  void removeAttribute(String name);

}
