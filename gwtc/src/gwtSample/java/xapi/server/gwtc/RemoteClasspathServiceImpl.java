package xapi.server.gwtc;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import xapi.annotation.inject.SingletonDefault;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.gwtc.compiler.model.ClasspathEntry;
import xapi.inject.X_Inject;
import xapi.fu.Lazy;
import xapi.log.X_Log;

@SingletonDefault(implFor=RemoteClasspathService.class)
public class RemoteClasspathServiceImpl implements RemoteClasspathService{

  protected static final Pattern GWT_XML_PATTERN =
      Pattern.compile(".*gwt[.]*xml");

  protected final ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
  
  protected final Lazy<String> gwtcHome = Lazy.deferred1(() -> {
      return System.getProperty(GWTC_HOME);
  });
  
  protected final Lazy<Map<String, String>> gwtModules = Lazy.deferred1(() -> {
      Map<String, String> map = new HashMap<String, String>();
      ClasspathResourceMap resources = scanner.matchResource(".*[.]gwt[.]*xml")
          .scan(Thread.currentThread().getContextClassLoader());
      for (StringDataResource resource : resources.findResources("", GWT_XML_PATTERN)) {
        try {
          Xpp3Dom dom = Xpp3DomBuilder.build(resource.open(), "UTF-8");
          String rename = dom.getAttribute("rename-to");
          if (rename != null) {
            String resName = resource.getResourceName().replace('/', '.');
            resName = resName.substring(0, resName.lastIndexOf("gwt")-1);
            map.put(rename, resName);
            X_Log.trace("Found gwt module rename; ", rename," -> ",resName);
          }
        } catch (Exception e) {
          X_Log.error("Error reading xml from ",resource.getResourceName(),e
              ,"\nSet xapi.log.level=TRACE to see the faulty xml");
          X_Log.trace("Faulty xml: ",resource.readAll());
        }
      };
      return map;
  });
  
  @Override
  public ClasspathEntry[] getClasspath(String forModule, HttpServletRequest req) {
    boolean isShortName = forModule.indexOf('.') == -1;
    String longName = isShortName ? getLongName(forModule) : forModule;
    String home = gwtcHome.out1();
    if (home == null) {
      return classpathFromClassloader(longName, Thread.currentThread().getContextClassLoader());
    }
    return null;
  }

  protected ClasspathEntry[] classpathFromClassloader(String longName, ClassLoader classLoader) {
    URL res = classLoader.getResource("META-INF/"+longName);
    if (res == null) {
    }
    return null;
  }

  protected String getLongName(String forModule) {
    Map<String, String> map = gwtModules.out1();
    return map.containsKey(forModule) ? map.get(forModule) : forModule;
  }

  @Override
  public void setClasspath(String forModule, ClasspathEntry[] classpath,
      HttpServletRequest threadLocalRequest) {
    
  }

}
