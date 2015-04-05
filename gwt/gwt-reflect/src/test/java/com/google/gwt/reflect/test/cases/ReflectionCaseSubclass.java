package com.google.gwt.reflect.test.cases;


@SuppressWarnings("unused")
public class ReflectionCaseSubclass extends ReflectionCaseSuperclass {
  // Exact same fields as super class so we can test behavior
  private boolean privateCall;
  public boolean publicCall;

  private void privateCall(final long l) {
    privateCall = true;
  }

  public void publicCall(final Long l) {
    publicCall = true;
  }

}