package com.google.gwt.reflect.test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.MemberPool;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class TestEntryPoint implements EntryPoint {

  private final Map<Method,Object> tests = new LinkedHashMap<Method,Object>();

  @Override
  public void onModuleLoad() {
    String module = GWT.getModuleName(), host = GWT.getHostPageBaseURL().replace("/"+module, "");
    debug("<a href='#' onclick=\""
          + "window.__gwt_bookmarklet_params = "
            + "{server_url:'" + host+ "', "
            + "module_name:'" + module + "'}; "
          + "var s = document.createElement('script'); "
          + "s.src = 'http://localhost:1337/dev_mode_on.js'; "
          + "document.getElementsByTagName('head')[0].appendChild(s); "
          + "return true;"
        + "\">Recompile</a>", null);
    
    GWT.runAsync(TestEntryPoint.class, new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        GwtReflect.magicClass(TestEntryPoint.class);
        MethodTests.class.getName();
        GwtReflect.magicClass(AnnotationTests.class);
        try {
          String.class.getMethod("equals", Object.class).invoke("!", "!");
        } catch (Exception e) {debug("oops", e);
          }
        ConstPool.loadConstPool(new AsyncCallback<ConstPool>() {
          @Override
          public void onSuccess(ConstPool result) {
            for (MemberPool<?> m : result.getAllReflectionData()) {
              try {
                Class<?> c = m.getType();
                addTests(c);
              } catch (Throwable e) {
                debug("Error adding tests", e);
              }
            }
            displayTests();
            runTests();
          }

          @Override
          public void onFailure(Throwable caught) {

          }
        });

      }

      @Override
      public void onFailure(Throwable reason) {

      }
    });

  }

  private void addTests(Class<?> cls) throws Throwable {
    Iterable<Method> allTests = JUnit4Test.findTests(cls);
    if (allTests.iterator().hasNext()) {
      Object inst = cls.newInstance();
      for (Method method : allTests) {
        tests.put(method, inst);
      }
    }
  }

  private void displayTests() {
    BodyElement body = Document.get().getBody();
    for (final Method m : tests.keySet()) {
      DivElement div = Document.get().createDivElement();
      StringBuilder b = new StringBuilder();
      b.append("<pre>");
      b.append("<a href='javascript:'>");
      b.append(m.getDeclaringClass().getName());
      b.append('.');
      b.append(m.getName());
      b.append("</a>");
      b.append('(');
      b.append(GwtReflect.joinClasses(", ", m.getParameterTypes()));
      b.append(')');
      b.append("</pre>");
      try {
        b.append("<sup>")
          .append(m.getDeclaringClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm())
          .append("</sup>");
      } catch (Exception ignored) {}
      div.setInnerHTML(b.toString());
      DOM.setEventListener(div.<Element>cast(), new EventListener() {
        @Override
        public void onBrowserEvent(Event event) {
          if (event.getTypeInt() == Event.ONCLICK) {
            runTest(m);
          }
        }
      });
      DOM.sinkEvents(div.<Element>cast(), Event.ONCLICK);
      body.appendChild(div);
    }
  }

  private void runTests() {
    int delay = 1;
    for (final Method method : tests.keySet()) {
      new Timer() {

        @Override
        public void run() {
          runTest(method);
        }
      }.schedule(delay += 100);
    }
  }

  protected void runTest(Method m) {
    try {
      JUnit4Test.runTest(tests.get(m), m);
      debug("<div style='color:green'>" + m.getName() + " passes!</div>", null);
    } catch (Throwable e) {
      String error = m.getDeclaringClass().getName() + "." + m.getName() + " failed";
      while (e.getClass() == RuntimeException.class && e.getCause() != null)
        e = e.getCause();
      debug(error, e);
      throw new AssertionError(error);
    }
  }

  private void debug(String string, Throwable e) {
    DivElement el = Document.get().createDivElement();
    StringBuilder b = new StringBuilder();
    b.append(string);
    b.append('\n');
    b.append("<pre style='color:red;'>");
    while (e != null) {
      b.append(e);
      b.append('\n');
      for (StackTraceElement trace : e.getStackTrace()) {
        b.append('\t')
          .append(trace.getClassName())
          .append('.')
          .append(trace.getMethodName())
          .append(' ')
          .append(trace.getFileName())
          .append(':')
          .append(trace.getLineNumber())
          .append('\n');
      }
      e = e.getCause();
    }
    b.append("</pre>");

    el.setInnerHTML(b.toString());
    Document.get().getBody().appendChild(el);
  }
}
