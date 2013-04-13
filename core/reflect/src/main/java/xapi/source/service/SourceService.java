package xapi.source.service;

import xapi.source.api.IsClass;
import xapi.source.api.IsType;

public interface SourceService {

  IsClass parseClass(byte[] bytecode);
  IsClass parseClass(String source);

  IsType toType(Class<?> cls);
  IsType toType(String pkg, String enclosedName);

  char classSeparator();
  char packageSeparator();

}
