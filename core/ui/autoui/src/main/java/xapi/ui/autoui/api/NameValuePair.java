package xapi.ui.autoui.api;

public class NameValuePair <T> implements HasNamedValue<T> {

  private String name;
  private T value;

  public NameValuePair(String name, T value) {
    this.name = name;
    this.value = value;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public T getValue() {
    return value;
  }

}
