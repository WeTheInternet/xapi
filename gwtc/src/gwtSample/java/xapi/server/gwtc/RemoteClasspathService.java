package xapi.server.gwtc;

import javax.servlet.http.HttpServletRequest;

import xapi.gwtc.compiler.model.ClasspathEntry;

public interface RemoteClasspathService {

  String GWTC_HOME = "gwtc.home";
  
  ClasspathEntry[] getClasspath(String forModule, HttpServletRequest req);

  void setClasspath(String forModule, ClasspathEntry[] classpath,
      HttpServletRequest threadLocalRequest);
  
}
