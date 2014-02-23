package com.google.gwt.reflect.shared;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.JavaScriptObject;

public class AnnotationMap extends JavaScriptObject {

  protected AnnotationMap() {}

  public final boolean hasAnnotation(Class<? extends Annotation> annoClass) {
    return ReflectUtil.hasMember(annoClass.getName(), this);
  }

  public final <T extends Annotation> T getAnnotation(Class<T> annoClass) {
    return ReflectUtil.getOrMakePublicMember(annoClass.getName(), this);
  }

  public final <T extends Annotation> T getDeclaredAnnotation(Class<T> annoClass) {
    return ReflectUtil.getOrMakeDeclaredMember(annoClass.getName(), this);
  }

  public final Annotation[] getAnnotations() {
    return ReflectUtil.getPublicMembers(this, new Annotation[0]);
  }

  public final Annotation[] getDeclaredAnnotations() {
    return ReflectUtil.getDeclaredMembers(this, new Annotation[0]);
  }

}
