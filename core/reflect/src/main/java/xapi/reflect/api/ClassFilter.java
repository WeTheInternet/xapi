package xapi.reflect.api;


public class ClassFilter {

  public boolean accept(Class<?> cls) {
    return true;
  }

  public boolean accept(String fileName) {
    return true;
  }
}
