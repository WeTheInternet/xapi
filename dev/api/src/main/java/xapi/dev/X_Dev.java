package xapi.dev;

import xapi.dev.resource.impl.FileBackedResource;
import xapi.dev.resource.impl.StringDataResource;
import xapi.fu.X_Fu;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.constants.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class X_Dev {

	private X_Dev(){}

	public static URL toUrl(String url) {
    // Normalize
    if (!url.endsWith(File.separator)) {
      if (!url.endsWith(".jar")) {
        url += File.separator;
      }
    }
    if (!url.matches("[a-z]*:.*")||url.matches("[a-z]:[\\\\].*")) {
        url = "file:"+url;
    }
    // toUrl
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
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
      if (ind == -1) {
        return loc;
      }
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

    public static URL[] getUrls(ClassLoader cl) {
        URL[] urls = new URL[0];
        while (cl instanceof URLClassLoader) {
            if (cl instanceof URLClassLoader) {
                final URL[] moreUrls = ((URLClassLoader) cl).getURLs();
                urls = X_Fu.concat(urls, moreUrls);
            }
            cl = cl.getParent();
        }
        return urls;
    }

    public static String ensureProtocol(String s) {
	    return ensureProtocol(s, false);
    }
    public static String ensureProtocol(String s, boolean mustBeFile) {
        if (s == null) {
            return null;
        }
        if (s.indexOf(':') == -1) {
            if (new File(s).isDirectory()) {
                return "file:" +
                    // URLClassLoader requires directories to end with /
                    (s.endsWith("/") ? s : s + "/");
            } else if (new File(s).isFile()) {
                return "file:" + s;
            } else {
                if (mustBeFile) {
                    X_Log.warn(X_Dev.class, "No file found for", s, "ignoring...");
                    return null;
                }
                // not actually a file... assume url.
                // make it https so anyone who actually wants
                // to serve data in plain text must opt in to doing so.
                return "https:" +
                    (s.startsWith("//") ? s : "//" + s);
            }
        }
        return s;
    }
}
