package xapi.dev.gwtc.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;

import java.net.URLClassLoader;

public abstract class GwtcServiceAbstract extends ReflectiveMavenLoader implements GwtcService {

  protected class Replacement {
    protected String newValue;
    protected int start, end;

    public Replacement(int start, int end, String path) {
      this.start = start;
      this.end = end;
      newValue = path;
    }
  }

  protected final StringTo<GwtcProjectGenerator> projects;

  public GwtcServiceAbstract() {
    projects = X_Collect.newStringMap(GwtcProjectGenerator.class);

  }

  @Override
  public GwtcProjectGenerator getProject(String moduleName, ClassLoader resources) {
    final ClassLoader loader = resources == null ? Thread.currentThread().getContextClassLoader() : resources;
    return projects.getOrCreateFrom(moduleName, GwtcProjectGeneratorDefault::new, this, loader, moduleName);
  }

  protected void doLog(LogLevel level, String msg) {
    X_Log.log(GwtcServiceAbstract.class, level, msg);
  }

  public void error(String log) {
    doLog(LogLevel.ERROR, log);
  }

  public void warn(String log) {
    doLog(LogLevel.WARN, log);
  }

  public void info(String log) {
    doLog(LogLevel.INFO, log);
  }

  public void trace(String log) {
    doLog(LogLevel.TRACE, log);
  }

  public void debug(String log) {
    doLog(LogLevel.DEBUG, log);
  }

  @Override
  public String extractGwtVersion(String gwtHome) {
    int lastInd = gwtHome.lastIndexOf("gwt-dev");
    gwtHome = gwtHome.substring(lastInd+7).replace(".jar", "");
    return gwtHome.startsWith("-") ? gwtHome.substring(1) : gwtHome;
  }

}
