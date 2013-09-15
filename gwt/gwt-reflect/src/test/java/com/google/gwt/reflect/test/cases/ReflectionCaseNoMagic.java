package com.google.gwt.reflect.test.cases;

public class ReflectionCaseNoMagic {
  
  public static class Subclass extends ReflectionCaseNoMagic {
    protected boolean overrideField;// shadows the superclass field
    
    public static boolean getOverrideField(Subclass s) {
      return s.overrideField;
    }
  }
  
  public ReflectionCaseNoMagic() {}

  private boolean privateCall;
  public boolean publicCall;
  public boolean overrideField;
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
  
  public boolean overrideField() {
    return this.overrideField;
  }
  
}
