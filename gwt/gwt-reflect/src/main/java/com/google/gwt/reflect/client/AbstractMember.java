package com.google.gwt.reflect.client;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is the super class of the super-sourced reflection members,
 * Constructor, Field and Method.
 * 
 * It exposes all shared functionality used 
 * 
 * 
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public class AbstractMember extends AccessibleObject implements Member {

  private final JavaScriptObject annos;
  private final Class<?> declaringClass;
  private final int modifiers;
  private final String name;

  protected AbstractMember(Class<?> enclosingClass, int modifiers, String name, JavaScriptObject annos) {
    this.annos = annos;
    this.declaringClass = enclosingClass;
    this.modifiers = modifiers;
    this.name = name;
  }

  @Override
  public final Class<?> getDeclaringClass() {
    return declaringClass;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final int getModifiers() {
    return modifiers;
  }

  @Override
  public final boolean isSynthetic() {
    return false;
  }
  
}
