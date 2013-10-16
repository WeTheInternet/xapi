package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.reflect.rebind.generators.ConstructorGenerator;

public class DeclaredConstructorInjector extends ConstructorGenerator {

  @Override
  protected boolean isDeclared() {
    return true;
  }

}
