package com.google.gwt.reflect.test.cases;

public class ReflectionCaseNoMagic {

  public ReflectionCaseNoMagic() {}

  private boolean privateCall;
  public boolean publicCall;
  boolean _boolean;
  byte _byte;
  short _short;
  char _char;
  int _int;
  long _long;
  float _float;
  double _double;
  
  @SuppressWarnings("unused")
  private void privateCall() { privateCall = true; }
  public void publicCall() { publicCall = true; }

  public boolean wasPrivateCalled(){return privateCall;}
  
}
