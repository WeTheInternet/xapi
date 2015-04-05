package com.google.gwt.reflect.test.cases;

import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.ALL_ANNOTATIONS;
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
    annotationRetention=ALL_ANNOTATIONS
    ,methodRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,fieldRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,constructorRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
    ,typeRetention=@GwtRetention(annotationRetention=COMPILE|RUNTIME)
)
public class ReflectionCaseHasAllAnnos extends ReflectionCaseSuperclass {

  protected ReflectionCaseHasAllAnnos() {}

  @SourceRetention
  @CompileRetention
  @RuntimeRetention
  public ReflectionCaseHasAllAnnos(
    @SourceRetention
    @CompileRetention
    @RuntimeRetention
    final
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
    final
    Long param
  ) { return field; }

}
