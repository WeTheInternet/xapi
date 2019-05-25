package xapi.dev.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import xapi.log.X_Log;
import xapi.util.X_Properties;
import xapi.util.X_Runtime;

public class ClasspathFixer extends ThreadLocal<ClassLoader>{

  @Override
  public synchronized ClassLoader get() {
    return super.get();
  }

  private static final Pattern uberMatcher = Pattern.compile(
    ".*xapi-gwt-(([0-9][.]*)(-SNAPSHOT)*).*"
  );

  @Override
  protected ClassLoader initialValue() {

    String gwtArgs = X_Properties.getProperty("gwt.args", "");
    String flag = X_Properties.getProperty("xapi.gwt.testMode", "prod");
    if ("prod".equals(flag)) {
      if (!gwtArgs.contains("-prod")) {
        gwtArgs = "-prod "+gwtArgs;
      }
    }


    flag = X_Properties.getProperty("xapi.log.level", "INFO");
    if (gwtArgs.contains("-logLevel")) {
      gwtArgs = gwtArgs.replaceAll("logLevel ([A-Z]+)", "logLevel "+flag);
    } else {
      gwtArgs = "-logLevel "+flag+" "+gwtArgs;
    }

    flag = X_Properties.getProperty("xapi.gwt.batch", "module");
    if (!gwtArgs.contains("-batch")) {
      gwtArgs = "-batch "+flag+" "+gwtArgs;
    }

    flag = X_Properties.getProperty("xapi.gwt.precompile", "parallel");
    if (!gwtArgs.contains("-precompile")) {
      gwtArgs = "-precompile "+flag+" "+gwtArgs;
    }

    if (!gwtArgs.contains("-userAgents")) {
      flag = X_Properties.getProperty("xapi.user.agent", "gecko1_8,safari");
      gwtArgs = "-userAgents "+flag+" "+gwtArgs;
    }
    if (!gwtArgs.contains("-localWorkers")) {
      flag = X_Properties.getProperty("xapi.concurrent", "6");
      gwtArgs = "-localWorkers "+flag+" "+gwtArgs;
    }

    if (!gwtArgs.contains("-style")) {
      flag = X_Properties.getProperty("gwt.style", "PRETTY");
      gwtArgs = "-style "+flag+" "+gwtArgs;
    }

    flag = X_Properties.getProperty("xapi.selenium", "");
//    flag = X_Properties.getProperty("xapi.selenium", "Selenium:WeTheInternet:4444/*firefox");
    if (flag.length() > 0) {
      if (gwtArgs.contains("-runStyle")) {
        gwtArgs = gwtArgs.replaceAll("-runStyle ([^ ]+) ", "runStyle "+flag+" ");
      } else {
        // TODO: test for selenium availability
        gwtArgs = "-runStyle "+flag+" " + gwtArgs;
      }
    }
//    if (!gwtArgs.contains("-quirksMode")) {
//      gwtArgs = "-standardsMode " + gwtArgs;
//    }

    if (X_Runtime.isDebug())
      System.out.println(gwtArgs);

    System.setProperty("gwt.usearchives", "false");
    System.setProperty("gwt.args", gwtArgs);

    Thread thread = Thread.currentThread();
    ClassLoader cl = thread.getContextClassLoader();
    ArrayList<URL> urls = new ArrayList<URL>();
    URL xapiApi = null, xapiUber = null;
    boolean findUber = false;
    String repo = System.getenv("M2_HOME");

    while (cl != null) {
      if (cl instanceof URLClassLoader) {
        URL[] cp = ((URLClassLoader)cl).getURLs();
        for (URL url : cp) {

          String to = url.toExternalForm();
          if (to.contains("xapi-api")) {
            xapiApi = url;
          } else if (uberMatcher.matcher(to).matches()) {
            xapiUber = url;
          } else if (to.contains("xapi"+File.separator+"gwt"+File.separator+"uber"
            +File.separator+"target")) {
            // uh-oh! we have the uber project open in eclipse.  Must find maven repo
            urls.add(url);
            findUber = true;

            try {
              urls.add(new URL(to.replaceAll("target[\\\\/]test-classes" ,
                "src"+File.separator+"test"+File.separator+"java"
                )));
              urls.add(new URL(to.replaceAll("target[\\\\/]test-classes" ,
                "src"+File.separator+"test"+File.separator+"resources"
                )));
            } catch (MalformedURLException e) {
              e.printStackTrace();
            }

          } else if (to.contains(".m2" +File.separator+"repository")){
            if (repo == null) {
              assert to.startsWith("file:");
              repo = to.substring(5, to.indexOf(".m2"+File.separator+"repository")+15);
            }
            urls.add(url);
          } else {
            if ("file".equals(url.getProtocol())
              && to.endsWith("classes"+File.separator)) {
              if (to.contains("xapi"+File.separator+"gwt"+File.separator+"api")) {
                xapiApi = url;
                // add sources while we're here
                try {
                  urls.add(new URL(to.replaceAll("target[\\\\/]classes" ,
                    "src"+File.separator+"main"+File.separator+"java"
                    )));
                  urls.add(new URL(to.replaceAll("target[\\\\/]classes" ,
                    "src"+File.separator+"main"+File.separator+"resources"
                    )));
                } catch (MalformedURLException e) {
                  e.printStackTrace();
                }
                continue;
              }
              if (to.contains("xapi"+File.separator+"gwt"+File.separator+"uber")) {
                xapiUber = url;
                continue;
              }
              // fix eclipse junit by searching for source folders.
              int target = to.indexOf("target"+File.separator);
              if (target != -1) {
                try {
                  File base = new File (to.substring(5, target));
                  assert base.exists();
                  // add source, and resources
                  String isTest = to.contains("test-classes") ? "test" : "main";
                  File source = new File(base, "src/" + isTest + "/java/");
                  if (!source.exists()) {
                    source = new File(base, "src");
                  }
                  if (source.exists()) {
                    urls.add(source.toURI().toURL());
                  }
                  source = new File(base, "src/" + isTest + "/resources/");
                  if (source.exists()) {
                    urls.add(source.toURI().toURL());
                  }
                } catch (Throwable e) {
                  X_Log.error("Error looking up source classpath from "+to, e);
                }
              }
            }
            urls.add(url);
          }
        }
      }
      cl = cl.getParent();
    }

    if (xapiApi != null) {
      urls.add(0, xapiApi);
    }
    if (xapiUber != null) {
      urls.add(0, xapiUber);
    } else if (findUber) {
      if (repo == null) {
        X_Log.warn("You have added the xapi-gwt distribution project while it " +
      		"is open in eclipse.  This will add an empty classes folder, instead " +
      		"of the desired uber jar.  We tried to guess location from the repo, " +
      		"but we unable to find the desired jar.");
      } else {
        File root = new File(repo, "wetheinter/net/xapi-gwt/");
        X_Log.warn("TODO: finish looking up gwtUber from repo, by inspecting " +
        		"other items in the classpath to discover xapi version.", root);
      }
    }

    if (X_Runtime.isDebug())
      System.out.println("Using classpath:\n"+urls);

    return new URLClassLoader(
      urls.toArray(new URL[urls.size()]),
      thread.getContextClassLoader()
    );
  }

}
