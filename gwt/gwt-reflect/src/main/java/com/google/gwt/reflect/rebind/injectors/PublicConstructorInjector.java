package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.reflect.rebind.generators.ConstructorGenerator;

public class PublicConstructorInjector extends ConstructorGenerator implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return false;
  }

}
