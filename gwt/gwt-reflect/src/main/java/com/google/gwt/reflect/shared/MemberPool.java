package com.google.gwt.reflect.shared;

import com.google.gwt.core.client.SingleJsoImplName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SingleJsoImplName("com.google.gwt.reflect.shared.JsMemberPool")
public interface MemberPool<T> {

  <A extends Annotation> A getAnnotation(Class<A> annoCls);

  Annotation[] getAnnotations();

  Class<?>[] getClasses();

  Constructor<T> getConstructor(Class<?>... params) throws NoSuchMethodException;

  Constructor<T>[] getConstructors();

  <A extends Annotation> A getDeclaredAnnotation(Class<A> annoCls);

  Annotation[] getDeclaredAnnotations();

  Constructor<T> getDeclaredConstructor(Class<?>... params) throws NoSuchMethodException;

  Constructor<T>[] getDeclaredConstructors();

  Field getDeclaredField(String name) throws NoSuchFieldException;

  Field[] getDeclaredFields();

  Method getDeclaredMethod(String name, Class<?>... params) throws NoSuchMethodException;

  Method[] getDeclaredMethods();

  Field getField(String name) throws NoSuchFieldException;

  Field[] getFields();

  Class<?>[] getInterfaces();

  Method getMethod(String name, Class<?>... params) throws NoSuchMethodException;

  Method[] getMethods();

  MemberPool<? super T> getSuperclass();

  Class<T> getType();

}
