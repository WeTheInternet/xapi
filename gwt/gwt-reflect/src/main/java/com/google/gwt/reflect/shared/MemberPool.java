package com.google.gwt.reflect.shared;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.gwt.core.client.SingleJsoImplName;

@SingleJsoImplName("com.google.gwt.reflect.shared.JsMemberPool")
public interface MemberPool <T> {

  <A extends Annotation> A getAnnotation(Class<A> annoCls);
  Annotation[] getAnnotations();
  Annotation[] getDeclaredAnnotations();

  Constructor<T> getConstructor(Class<?> ... params) throws NoSuchMethodException;
  Constructor<T> getDeclaredConstructor(Class<?> ... params) throws NoSuchMethodException;
  Constructor<T>[] getConstructors();
  Constructor<T>[] getDeclaredConstructors();

  Field getField(String name) throws NoSuchFieldException;
  Field getDeclaredField(String name) throws NoSuchFieldException;
  Field[] getFields();
  Field[] getDeclaredFields();

  Method getMethod(String name, Class<?> ... params) throws NoSuchMethodException;
  Method getDeclaredMethod(String name, Class<?> ... params) throws NoSuchMethodException;
  Method[] getMethods();
  Method[] getDeclaredMethods();

  MemberPool<? super T> getSuperclass();
  Class<?>[] getInterfaces();
  Class<?>[] getClasses();
  Class<T> getType();

}
