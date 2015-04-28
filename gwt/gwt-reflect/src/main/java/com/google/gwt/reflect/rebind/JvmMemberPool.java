package com.google.gwt.reflect.rebind;

import com.google.gwt.core.client.JavaScriptObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JvmMemberPool <T> extends JavaScriptObject {

  private final Class<T> type;

  public JvmMemberPool(final Class<T> type) {
    this.type = type;
  }

  public <A extends Annotation> A getAnnotation(final Class<A> annoCls) {
    return type.getAnnotation(annoCls);
  }

  public Annotation[] getAnnotations() {
    return type.getAnnotations();
  }

  public Annotation[] getDeclaredAnnotations() {
    return type.getDeclaredAnnotations();
  }

  public <A extends Annotation> A getDeclaredAnnotation(final Class<A> annoCls) {
    return type.getDeclaredAnnotation(annoCls);
  }

  public Constructor<T> getConstructor(final Class<?> ... params) throws NoSuchMethodException {
    return type.getConstructor(params);
  }

  public Constructor<T> getDeclaredConstructor(final Class<?> ... params) throws NoSuchMethodException {
    return type.getDeclaredConstructor(params);
  }

  @SuppressWarnings("unchecked")
  public Constructor<T>[] getConstructors() {
    return (Constructor<T>[])type.getConstructors();
  }

  @SuppressWarnings("unchecked")
  public Constructor<T>[] getDeclaredConstructors() {
    return (Constructor<T>[])type.getDeclaredConstructors();
  }

  public Field getField(final String name) throws NoSuchFieldException {
    return type.getField(name);
  }

  public Field getDeclaredField(final String name) throws NoSuchFieldException {
    return type.getDeclaredField(name);
  }

  public Field[] getFields() {
    return type.getFields();
  }

  public Field[] getDeclaredFields() {
    return type.getDeclaredFields();
  }

  public Method getMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    return type.getMethod(name, params);
  }

  public Method getDeclaredMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    return type.getDeclaredMethod(name, params);
  }

  public Method[] getMethods() {
    return type.getMethods();
  }

  public Method[] getDeclaredMethods() {
    return type.getDeclaredMethods();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public JvmMemberPool<? super T> getSuperclass() {
    return new JvmMemberPool(type.getSuperclass());
  }

  public Class<?>[] getInterfaces() {
    return type.getInterfaces();
  }

  public Class<?>[] getClasses() {
    return type.getClasses();
  }

  public Class<T> getType() {
    return type;
  }

}
