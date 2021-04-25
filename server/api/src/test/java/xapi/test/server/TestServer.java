package xapi.test.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import xapi.bytecode.ClassFile;
import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.StringMemberValue;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.server.X_Server;
import xapi.server.annotation.XapiServlet;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.constants.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Runtime;
import xapi.util.X_Util;


public class TestServer
{

  Server server;

  private final int port;

  public TestServer() {
    int p[] = {0};
    X_Server.usePort(newPort->
        server = new Server(p[0] = newPort)
    );
    X_Properties.setProperty(X_Namespace.PROPERTY_SERVER_PORT,
        Integer.toString(port=p[0])
    );
    // When testing, we don't want to setup configuration files,
    // we want to write code and run-it-right-now.
    // So, we scan the classpath for instances of HttpServlet,
    // and we mount everything we find.
    scanForServlets();
  }

  /**
   * Performs runtime scan for servlets.  For integration tests, you
   * probably don't want your tests to load resources they can't load in production.
   */
  @SuppressWarnings("unchecked")
  protected void scanForServlets() {

    if (X_Runtime.isRuntimeInjection()) {
      final ClassLoader cl = getClassloader();
      final ClasspathResourceMap resources = X_Inject.instance(ClasspathScanner.class)
        .scanAnnotation(XapiServlet.class)
        .matchClassFile(".*")
      .scan(cl);
      final boolean debug = X_Runtime.isDebug();
      final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/xapi");
      context.setClassLoader(cl);
      final Moment start = X_Time.now();
      for (final ClassFile cls : resources.findClassAnnotatedWith(XapiServlet.class)) {
        final Annotation a = cls.getAnnotation(XapiServlet.class.getName());
        final StringMemberValue prefix = (StringMemberValue)a.getMemberValue("prefix");
        if (debug) {
          X_Log.info("Found XapiServlet "+cls.getName()+" mounted at /xapi/"+prefix.getValue());
        }
        String fragment = prefix.getValue();
        if (fragment.length() == 0 || fragment.charAt(0) != '/') {
          fragment = "/"+fragment;
        }
        // TODO: run injection on the servlet types
        // TODO check if this servlet is registered in xapi.xml or not.
        try {
          context.addServlet(cls.getName(), fragment);
        } catch (final Throwable e) {
          X_Log.error("Error starting servlet "+cls.getName()+" @ "+fragment);
        }

      }
      final ContextHandlerCollection all = new ContextHandlerCollection();
      all.addHandler(context);
      all.addHandler(new DefaultHandler());
      server.setHandler(all);
      if (X_Log.loggable(LogLevel.DEBUG)) {
        X_Log.trace("Scanned XApiServlet annotations in "+X_Time.difference(start)+" ");
      }
    }
  }

  protected ClassLoader getClassloader() {
    return Thread.currentThread().getContextClassLoader();
  }

  protected int getPort() {
    return port;
  }

  /**
   * Called when the server is ready.
   */
  protected void onReady() {

  }

  public void start() {
    if (server.isStarted()) {
      return;
    }
    try {
      server.start();
    } catch (final Exception e) {
      X_Log.error("Test server could not start", e);
      throw X_Util.rethrow(e);
    }
  }

  public void finish() {

    try {
      server.stop();
    } catch (final Exception e) {
      X_Log.warn("Failure stopping server: ", e);
      Thread.currentThread().interrupt();
    } finally {
      server.destroy();
    }
  }

}
