package com.google.gwt.reflect.test.cases;

import com.google.gwt.reflect.test.annotations.RuntimeRetention;

public class ReflectionCaseNoMagic {

  public static class Subclass extends ReflectionCaseNoMagic {
    protected boolean overrideField;// shadows the superclass field

    public static boolean getOverrideField(final Subclass s) {
      return s.overrideField;
    }
    public Subclass() {}

    protected Subclass(final String s) {
      super(s+"1");
    }

    public Subclass(final long l) {
      super(l+1);
    }
  }

  public ReflectionCaseNoMagic() {}
  protected ReflectionCaseNoMagic(final String s) {
    _String = s;
  }
  private ReflectionCaseNoMagic(final long l) {
    this._long = l;
  }

  @RuntimeRetention
  private boolean privateCall;
  @RuntimeRetention
  public boolean publicCall;
  public boolean overrideField;
  boolean _boolean;
  byte _byte;
  short _short;
  char _char;
  int _int;
  public long _long;
  float _float;
  double _double;
  public String _String;

  public static final String PUBLIC_CALL = "publicCall";

  @SuppressWarnings("unused")
  private void privateCall() { privateCall = true; }
  public void publicCall() { publicCall = true; }

  public boolean wasPrivateCalled(){return privateCall;}

  public boolean overrideField() {
    return this.overrideField;
  }

}
