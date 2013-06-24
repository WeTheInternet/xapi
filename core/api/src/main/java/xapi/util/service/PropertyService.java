package xapi.util.service;

public interface PropertyService {

  String getProperty(String key);
  String getProperty(String key, String dflt);
  void setProperty(String key, String value);
  boolean isRuntimeInjection();
}
