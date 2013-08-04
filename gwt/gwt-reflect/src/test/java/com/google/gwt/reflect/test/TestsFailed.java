package com.google.gwt.reflect.test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

public class TestsFailed extends Exception {

  private static final long serialVersionUID = 2797492826431755971L;
  
  private final Map<Method, Throwable> results;

  public TestsFailed(Map<Method, Throwable> result) {
    super(serialize(result));
    this.results = result;
  }

  private static String serialize(Map<Method, Throwable> result) {
    StringBuilder b = new StringBuilder();
    for (Entry<Method, Throwable> e : result.entrySet()) {
      if (e.getValue() == null) {
        b.append(e.getKey().getName()).append(" (pass)\n");
      } else {
        b.append(e.getKey().getName()).append(" (FAIL):\n");
        print(b, e.getValue());
      }
    }
    return b.toString();
  }

  private static void print(StringBuilder b, Throwable e) {
    while (e != null) {
      b.append(e).append("\n");
      for (StackTraceElement trace : e.getStackTrace()) {
        b.append("\t ").append(trace).append("\n");
      }
      e = e.getCause();
    }
  }

  public Map<Method, Throwable> getResults() {
    return results;
  }

}