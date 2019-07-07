package org.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Emulate this common junit annotation so it can be used in gwt test platforms w/out error.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Test {

  /**
   * Default empty exception
   */
  static class None extends Throwable {
    private static final long serialVersionUID= 1L;
    private None() {
    }
  }

  /**
   * Optionally specify <code>expected</code>, a Throwable, to cause a test method to succeed iff
   * an exception of the specified class is thrown by the method.
   */
  Class<? extends Throwable> expected() default None.class;

  /**
   * Optionally specify <code>timeout</code> in milliseconds to cause a test method to fail if it
   * takes longer than that number of milliseconds.*/
  long timeout() default 0L;
}
