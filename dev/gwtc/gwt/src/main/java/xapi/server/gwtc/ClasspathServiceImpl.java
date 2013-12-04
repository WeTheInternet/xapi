package xapi.server.gwtc;

import xapi.gwtc.compiler.model.ClasspathEntry;
import xapi.gwtc.service.ClasspathService;
import xapi.inject.X_Inject;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ClasspathServiceImpl extends RemoteServiceServlet implements ClasspathService{

  private static final long serialVersionUID = -2745968766501288589L;

  @Override
  public ClasspathEntry[] getClasspath(String forModule) {
    RemoteClasspathService service = X_Inject.singleton(RemoteClasspathService.class);
    return service.getClasspath(forModule, getThreadLocalRequest());
  }

  @Override
  public void setClasspath(String forModule, ClasspathEntry[] classpath) {
    RemoteClasspathService service = X_Inject.singleton(RemoteClasspathService.class);
    service.setClasspath(forModule, classpath, getThreadLocalRequest());
  }

  @Override
  public ClasspathEntry[] browseRemote(String directory) {
    return new ClasspathEntry[0];
  }

  @Override
  public ClasspathEntry[] browseMaven(String groupId, String artifactId) {
    return new ClasspathEntry[0];
  }

}
