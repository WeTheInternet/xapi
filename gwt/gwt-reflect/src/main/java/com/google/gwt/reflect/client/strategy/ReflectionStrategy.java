package com.google.gwt.reflect.client.strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReflectionStrategy {

  // Annotation retention
  final int 
      NONE = 0,
      COMPILE = 1,
      RUNTIME = 2;
  
  // Member/type ids
  final int 
    TYPE = 1,
    CONSTRUCTOR = 2,
    FIELD = 4,
    METHOD = 8,
    ANNOTATION = 0x10,
    META = 0x20,
    SOURCE = 0x40,
    ALL = 0x7f;

  /**
   * Set to true to retain all ancestors' public fields.
   * <p>
   * Ancestors (supertypes and interfaces) will be checked for public members,
   * and will have reflection data written for those members (via clinit on
   * supertypes) when the direct .getMethod(), .getMethods(), etc is called
   * (versus the more precise .getDeclaredMethod(), .getDeclaredMethods()).
   * 
   * @return true to cause all superclasses to be enhanced.
   */
  boolean magicSupertypes() default false;

  // not yet implemented
  //  boolean magicSubtypes() default false;

  /**
   * Simple flag to mark a type (or package) that should keep zero reflection data.
   */
  boolean keepNothing() default false;

  /**
   * Simple flag to mark a type (or package) that should keep all reflection data.
   * <p>
   * USE AT YOUR OWN RISK.  Use -compileReport and -extra to view how much code
   * is added when you apply this setting.  In the case of large classes that
   * would be pruned, this could easily triple the amount of compiled output.
   * <p>
   * That said, if you want a complete jvm-compatible class without tweaking anything,
   * just set this to true (but do try to avoid doing so at the package level).
   */
  boolean keepEverything() default false;
  
  /**
   * Whether or not to include the file url to the source file, as returned by
   * Class.class.getProtectionDomain().getCodeSource().getLocation().getPath();
   * <p>
   * Ya, it's pretty ugly, but that's how to get a classes compiled location
   * in any jvm that supports it.
   * <p>
   * <b>Setting this value to true imposes a security risk!</b>
   * <p>
   * This will compile locations of files on your harddrive into your application.
   * If you do not understand the security implications of this, you should NOT
   * use this in any production application.
   * @return
   */
  boolean keepCodeSource() default false;
  /**
   * Whether or not to automatically retain inner classes.
   * <p>
   * The default of false is to help reduce default code size;
   * you can annotate your subtypes manually; this just makes
   * it easier to paint with a broader brush.
   *
   * @return
   */
  boolean keepInnerTypes() default false;
  /**
   * @return a class which fulfills the {@link NewInstanceStrategy} contract.
   * <p>
   * Allow for easy injection of new instance strategies; provided defaults
   * are new Type(), and GWT.create(Type.class);.
   * <p>
   * Default is new Type(), or throw Exception() if no suitable constructor is available.
   */
  Class<? extends NewInstanceStrategy> newInstanceStrategy() default UseNewKeyword.class;

  /**
   * Define a retention policy for all methods within the annotated class or package.
   * <p>
   * A class-level {@link ReflectionStrategy} completely overrides a package level one.
   * <p>
   * The default retention strategy is to synchronously include all declared members,
   * and no supertype members.
   *
   */
  GwtRetention methodRetention() default @GwtRetention();
  GwtRetention constructorRetention() default @GwtRetention();
  GwtRetention fieldRetention() default @GwtRetention();
  GwtRetention typeRetention() default @GwtRetention();

  /**
   * An int register of annotation-retention level;
   * this sets the default for members within a given type;
   * you can annotate individual members with @GwtRetention.
   *
   * @return {@link #NONE} | {@link #COMPILE} | {@link #RUNTIME}
   */
  int annotationRetention() default NONE;
  /**
   * An int register of member types that should emit debug data.
   * 
   * @return an non-empty string causes the compiler to dump the generated reflection
   * source for this class.
   * 
   * Valid values are OR'd combinations of 
   * {@link ReflectionStrategy#TYPE}, {@link #CONSTRUCTOR}, {@link #FIELD}, {@link #METHOD} and {@link #ANNOTATION}
   */
  int debug() default NONE;
  
  /**
   * @return true to keep the package, false to elide
   */
  boolean keepPackage() default true;

}
