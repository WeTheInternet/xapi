package xapi.fu;

import java.lang.reflect.Type;

/**
 This package-local class implements our platform magic, Jutsu, and initializes it with all default methods.

 This allows you to use techniques like super-sourcing or source-rewriting
 to swap out this type with your own platform-specific overrides.

 See src/main/resource/xapi/jutsu/xapi/fu/Fu.java for the Gwt implementation of the super-sourcing technique
*/
class Fu implements Jutsu {
  static final Fu jutsu = getFu();

  private Fu(){}

  static Fu getFu() {
    return new Fu();
  }

  @Override
  public Type[] getGenericInterfaces(Class<?> c) {
    return c == null ? new Type[0] : c.getInterfaces();
  }
}
