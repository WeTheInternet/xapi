package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;

public class PublicFieldInjector extends AbstractFieldInjector implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return false;
  }

}
