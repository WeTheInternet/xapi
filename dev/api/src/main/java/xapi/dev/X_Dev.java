package xapi.dev;

import java.io.File;

import xapi.dev.scanner.ClasspathResourceMap;
import xapi.dev.scanner.ClasspathScanner;
import xapi.dev.scanner.StringDataResource;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Util;

public class X_Dev {

	private X_Dev(){}

	public static ClasspathResourceMap scanClassloader(ClassLoader cl) {
	    return X_Inject.instance(ClasspathScanner.class)
	      .matchClassFile(".*")
	      .matchResource(".*")
	      .matchSourceFile(".*")
	      .scanPackage("")
	      .scan(cl);
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
	
}
