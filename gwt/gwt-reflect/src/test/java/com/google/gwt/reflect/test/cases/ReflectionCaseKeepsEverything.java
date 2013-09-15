package com.google.gwt.reflect.test.cases;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;

@RuntimeRetention
@ReflectionStrategy(keepEverything=true)
public class ReflectionCaseKeepsEverything extends ReflectionCaseSuperclass{

  public ReflectionCaseKeepsEverything() {}
  
  @RuntimeRetention
  long privateCall;
  
  @CompileRetention
  Long publicCall;
  
  @CompileRetention private void privateCall() {
  }
  
  @RuntimeRetention public void publicCall() {
  }
}
