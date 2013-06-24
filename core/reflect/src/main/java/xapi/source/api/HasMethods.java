package xapi.source.api;

public interface HasMethods {

  Iterable<IsMethod> getMethods();
  Iterable<IsMethod> getDeclaredMethods();
  IsMethod getMethod(String name, IsType ... params);
  IsMethod getMethod(String name, boolean checkErased, Class<?> ... params);

}
