package com.google.gwt.reflect.rebind;

import com.google.gwt.reflect.shared.MemberPool;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JvmMemberPool <T> implements MemberPool<T>{

  private final Class<T> type;

  public JvmMemberPool(final Class<T> type) {
    this.type = type;
  }

  @Override
  public <A extends Annotation> A getAnnotation(final Class<A> annoCls) {
    return type.getAnnotation(annoCls);
  }

  @Override
  public Annotation[] getAnnotations() {
    return type.getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return type.getDeclaredAnnotations();
  }

  @Override
  public <A extends Annotation> A getDeclaredAnnotation(final Class<A> annoCls) {
    return type.getDeclaredAnnotation(annoCls);
  }

  @Override
  public Constructor<T> getConstructor(final Class<?> ... params) throws NoSuchMethodException {
    return type.getConstructor(params);
  }

  @Override
  public Constructor<T> getDeclaredConstructor(final Class<?> ... params) throws NoSuchMethodException {
    return type.getDeclaredConstructor(params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Constructor<T>[] getConstructors() {
    return (Constructor<T>[])type.getConstructors();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Constructor<T>[] getDeclaredConstructors() {
    return (Constructor<T>[])type.getDeclaredConstructors();
  }

  @Override
  public Field getField(final String name) throws NoSuchFieldException {
    return type.getField(name);
  }

  @Override
  public Field getDeclaredField(final String name) throws NoSuchFieldException {
    return type.getDeclaredField(name);
  }

  @Override
  public Field[] getFields() {
    return type.getFields();
  }

  @Override
  public Field[] getDeclaredFields() {
    return type.getDeclaredFields();
  }

  @Override
  public Method getMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    return type.getMethod(name, params);
  }

  @Override
  public Method getDeclaredMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    return type.getDeclaredMethod(name, params);
  }

  @Override
  public Method[] getMethods() {
    return type.getMethods();
  }

  @Override
  public Method[] getDeclaredMethods() {
    return type.getDeclaredMethods();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public MemberPool<? super T> getSuperclass() {
    return new JvmMemberPool(type.getSuperclass());
  }

  @Override
  public Class<?>[] getInterfaces() {
    return type.getInterfaces();
  }

  @Override
  public Class<?>[] getClasses() {
    return type.getClasses();
  }

  @Override
  public Class<T> getType() {
    return type;
  }

}
