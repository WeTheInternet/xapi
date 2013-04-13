package xapi.source.api;

public interface HasMethods {

  Iterable<IsMethod> getMethods();
  IsMethod getMethod(String name, IsType ... params);
  IsMethod getMethod(String name, Class<?> ... params);

}
