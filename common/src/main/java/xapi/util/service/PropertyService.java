package xapi.util.service;

import xapi.fu.Out1;

public interface PropertyService {

  String getProperty(String key);
  String getProperty(String key, String dflt);
  String getProperty(String key, Out1<String> dflt);
  void setProperty(String key, String value);
  boolean isRuntimeInjection();
}
