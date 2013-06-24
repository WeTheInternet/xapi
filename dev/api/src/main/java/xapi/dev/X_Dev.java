package xapi.dev;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import xapi.bytecode.ClassFile;
import xapi.dev.scanner.ClasspathResourceMap;
import xapi.dev.scanner.ClasspathScanner;
import xapi.dev.scanner.FileBackedResource;
import xapi.dev.scanner.StringDataResource;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_String;
import xapi.util.X_Util;

public class X_Dev {

	private X_Dev(){}

	public static ClasspathResourceMap scanFolder(String url) {
	  return scanClassloader(new URLClassLoader(new URL[]{toUrl(url)}));
  }
	public static ClasspathResourceMap scanFolder(String url,
	  boolean scanClasses, boolean scanSources, boolean scanResources, String pkg) {
	  return scanClassloader(
	      new URLClassLoader(new URL[]{toUrl(url)})
	    , scanClasses, scanSources, scanResources, pkg);
	}

  public static URL toUrl(String url) {
    // Normalize
    if (!url.endsWith(File.separator)) {
      if (!url.endsWith(".jar"))
        url += File.separator;
    }
    if (!url.matches("[a-z]*:.*")) {
        url = "file:"+url;
    }
    // toUrl
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static ClasspathResourceMap scanClassloader(ClassLoader cl) {
	    return X_Inject.instance(ClasspathScanner.class)
	      .matchClassFile(".*")
	      .matchResource(".*")
	      .matchSourceFile(".*")
	      .scanPackage("")
	      .scan(cl);
	  }
  
  public static ClasspathResourceMap scanClassloader(ClassLoader cl, 
      boolean scanClasses, boolean scanSources, boolean scanResources, String pkg) {
    ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
    if (scanClasses)
      scanner.matchClassFile(".*");
    if (scanResources)
      scanner.matchResource(".*");
    if (scanSources)
        scanner.matchSourceFile(".*");
    scanner.scanPackage(X_String.notNull(pkg));
    return scanner.scan(cl);
  }

	public static Iterable<StringDataResource> findPoms(ClassLoader cl) {
	    return X_Inject.instance(ClasspathScanner.class)
	      .matchResource("pom.*xml")
	      .scanPackage("")
	      .scan(cl)
	      .findResources("");
	  }

	public static Iterable<StringDataResource> findGwtXml(ClassLoader cl) {
	    return X_Inject.instance(ClasspathScanner.class)
	      .matchResource(".*gwt.*xml")// also match .gwtxml files
	      .scanPackage("")
	      .scan(cl)
	      .findResources("");
	  }

	public static Iterable<ClassFile> findEnums(ClassLoader cl) {
	  return X_Inject.instance(ClasspathScanner.class)
	      .matchClassFile(".*")
	      .scanPackage("")
	      .scan(cl)
	      .findDirectSubclasses(Enum.class);
	}
	
	public static synchronized File getXApiHome() {
	  // We purposely don't cache this value so users can change it at runtime.
	  String loc = X_Properties.getProperty(X_Namespace.PROPERTY_XAPI_HOME);
	  try {
	    
  	  if (loc == null) {
  	    // use a temporary directory instead
  	    File f = File.createTempFile("xapi", "home");
  	    loc = f.getAbsolutePath();
  	    X_Properties.setProperty(X_Namespace.PROPERTY_XAPI_HOME, loc);
  	    return f;
  	  }
  	  File home = new File(loc);
  	  if (!home.exists()) {
  	    X_Log.info("XApi home @ "+home.getCanonicalPath()+" does not exist.");
  	    if (home.mkdirs()) {
  	      X_Log.info("Successfully created home directory");
  	    } else {
  	      X_Log.warn("Unable to create home directory; using temp file.");
  	      X_Properties.setProperty(X_Namespace.PROPERTY_XAPI_HOME, null);
  	      return getXApiHome();
  	    }
  	  }
  	  return home;
	  } catch (Throwable e) {
	    throw X_Util.rethrow(e);
	  }
	}

  public static String readFile(File file) {
    return new StringDataResource(new FileBackedResource(file.getName(), file, 1)).readAll();
  }

  public static String toClasspath(URL cp) {
    try {
      String loc = cp.toURI().toString().replace("file:", "");
      int ind = loc.indexOf("jar!");
      if (ind == -1)
        return loc;
      return loc.substring(0, ind+3);
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }
  public static String[] toStrings(URL ... cp) {
    String[] urls = new String[cp.length];
    for (int i = 0, m = cp.length; i < m; i ++) {
      urls[i] = toClasspath(cp[i]);
    }
    return urls;
  }
	
}
