package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.reflect.rebind.generators.MethodGenerator;

public class PublicMethodInjector extends MethodGenerator implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return false;
  }

}
