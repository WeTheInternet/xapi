package com.google.gwt.reflect.test.cases;

import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.InheritedAnnotation;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;
import com.google.gwt.reflect.test.annotations.UninheritedAnnotation;

@RuntimeRetention
@UninheritedAnnotation
@InheritedAnnotation
@SuppressWarnings("unused")
public class ReflectionCaseSuperclass {

  @CompileRetention
  protected class InnerType {}

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

  private void privateCall() { privateCall = true; }
  public void publicCall() { publicCall = true; }

  public boolean wasPrivateCalled(){return privateCall;}

  boolean _boolean(){return _boolean;}
  byte _byte(){return _byte;}
  short _short(){return _short;}
  char _char(){return _char;}
  int _int(){return _int;}
  long _long(){return _long;}
  float _float(){return _float;}
  double _double(){return _double;}

}