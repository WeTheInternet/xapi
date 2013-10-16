package com.google.gwt.reflect.client;

import java.lang.annotation.Annotation;

public class AnnotationMap extends MemberMap{

  protected AnnotationMap() {}

  public final boolean hasAnnotation(Class<? extends Annotation> annoClass) {
    return hasMember(annoClass.getName(), this);
  }

  public final <T extends Annotation> T getAnnotation(Class<T> annoClass) {
    return getOrMakePublicMember(annoClass.getName(), this);
  }

  public final <T extends Annotation> T getDeclaredAnnotation(Class<T> annoClass) {
    return getOrMakeDeclaredMember(annoClass.getName(), this);
  }

  public final Annotation[] getAnnotations() {
    return getPublicMembers(this, new Annotation[0]);
  }

  public final Annotation[] getDeclaredAnnotations() {
    return getDeclaredMembers(this, new Annotation[0]);
  }

}
