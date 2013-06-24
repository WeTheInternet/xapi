package xapi.mojo.gwt;

import org.sonatype.aether.RepositorySystemSession;

import xapi.annotation.inject.SingletonOverride;
import xapi.mojo.api.AbstractXapiMojo;
import xapi.mvn.impl.MvnServiceDefault;
import xapi.mvn.service.MvnService;

@SingletonOverride(implFor=MvnService.class,priority=0)
public class MavenServiceMojo extends MvnServiceDefault{

  private static ThreadLocal<AbstractXapiMojo> mojos = new ThreadLocal<AbstractXapiMojo>();
  
  public static void init(AbstractXapiMojo mojo) {
    mojos.set(mojo);
  }

  public static void gc() {
    mojos.remove();
  }
  
  @Override
  protected RepositorySystemSession initLocalRepo() {
    AbstractXapiMojo mojo = mojos.get();
    if (mojo != null) {
      return mojo.getSession().getRepositorySession();
    }
    return super.initLocalRepo();
  }
  
  @Override
  public RepositorySystemSession getRepoSession() {
    AbstractXapiMojo mojo = mojos.get();
    if (mojo != null) {
      return mojo.getSession().getRepositorySession();
    }
    return super.getRepoSession();
  }
  
}
