package com.google.gwt.reflect.rebind;

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.reflect.client.strategy.GwtRetention;

public class ReflectionUnit <T extends HasAnnotations> {
  
  private final T node;
  private JMethod factory;
  private GwtRetention retention;
  
  public ReflectionUnit(T node, GwtRetention retention) {
    this.node = node;
    this.retention = retention;
  }

  public T getNode() {
    return node;
  }

  public JMethod getFactory() {
    return factory;
  }

  public void setFactory(JMethod factory) {
    this.factory = factory;
  }

  public GwtRetention getRetention() {
    return retention;
  }

  public void setRetention(GwtRetention retention) {
    this.retention = retention;
  }

}