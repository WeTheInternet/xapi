package xapi.gwtc.service;

import xapi.gwtc.compiler.model.ClasspathEntry;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ClasspathServiceIOAsync {

  void getClasspath(String forModule, AsyncCallback<ClasspathEntry[]> callback);

  void setClasspath(String forModule, ClasspathEntry[] classpath, AsyncCallback<Void> callback);
  
  void browseRemote(String directory, AsyncCallback<ClasspathEntry[]> callback);
  
  void browseMaven(String groupId, String artifactId, AsyncCallback<ClasspathEntry[]> callback);
  
}
