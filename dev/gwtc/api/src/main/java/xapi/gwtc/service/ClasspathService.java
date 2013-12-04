package xapi.gwtc.service;

import xapi.gwtc.compiler.model.ClasspathEntry;

public interface ClasspathService {

  ClasspathEntry[] getClasspath(String forModule);

  void setClasspath(String forModule, ClasspathEntry[] classpath);
  
  ClasspathEntry[] browseRemote(String directory);
  
  ClasspathEntry[] browseMaven(String groupId, String artifactId);
  
}
