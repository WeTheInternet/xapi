package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.reflect.rebind.generators.FieldGenerator;

public class PublicFieldInjector extends FieldGenerator implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return false;
  }

}
