package com.google.gwt.reflect.test.cases;

import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.COMPILE;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.RUNTIME;

import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;
import com.google.gwt.reflect.test.annotations.SourceRetention;


@SourceRetention
@CompileRetention
@RuntimeRetention
@ReflectionStrategy(
    annotationRetention=COMPILE|RUNTIME
    ,methodRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,fieldRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,constructorRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,typeRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
)
public class ReflectionCaseHasAllAnnos {

  protected ReflectionCaseHasAllAnnos() {}

  @SourceRetention
  @CompileRetention
  @RuntimeRetention
  public ReflectionCaseHasAllAnnos(
    @SourceRetention
    @CompileRetention
    @RuntimeRetention
    long param
  ) { }

  @SourceRetention
  @CompileRetention
  @RuntimeRetention
  private long field;

  @SourceRetention
  @CompileRetention
  @RuntimeRetention
  long method(
    @SourceRetention
    @CompileRetention
    @RuntimeRetention
    Long param
  ) { return field; }

}
