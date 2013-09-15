package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;

public class DeclaredMethodInjector extends AbstractMethodInjector implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return true;
  }

}
