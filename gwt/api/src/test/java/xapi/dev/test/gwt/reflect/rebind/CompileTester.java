package xapi.dev.test.gwt.reflect.rebind;

import static java.io.File.separator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import xapi.collect.impl.SimpleStack;

public class CompileTester {

  public void testCompile() {
    
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl instanceof URLClassLoader) {
      // collect classpath
      SimpleStack<String> paths = new SimpleStack<String>();
      SimpleStack<String> sources = new SimpleStack<String>();
      for (URL url : ((URLClassLoader)cl).getURLs()) {
        addPath(url.getPath(), paths);
      }
      paths = adjustClasspath(sources, paths);
    }
  }

  private void addPath(String path, SimpleStack<String> paths) {
    paths.add(path);
    if (path.endsWith("classes"+separator)) {
      int target = path.indexOf(separator+"target"+separator);
      if (target == -1) {
        path = path.replace(separator+"classes"+separator, separator+"src");
        if (new File(path).isDirectory())
          paths.add(path);
      } else {
        boolean isTest = path.endsWith("test-classes"+separator);
        String base = path.substring(0, target);
        if (isTest) {
          base += separator+"src"+separator + "test" + separator;
        } else {
          base += separator+"src"+separator + "main" + separator;
        }
        addResources(base, paths);
      }
    } else if (path.endsWith("bin"+separator)) {
      path = path.replace(separator+"bin"+separator, separator+"src");
      if (new File(path).isDirectory())
        paths.add(path);
    }
  }

  private void addResources(String base, SimpleStack<String> sources) {
    String folder = base + "java";
    if (new File(folder).isDirectory())
      sources.add(folder);
    folder = base + "resources";
    if (new File(folder).isDirectory())
      sources.add(folder);
  }

  // Let subclass modify the classpath
  protected SimpleStack<String> adjustClasspath(SimpleStack<String> sources, SimpleStack<String> classpath) {
    return sources.consume(classpath);
  }
  
  
}
