package com.google.gwt.reflect.client.strategy;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.reflect.client.GwtReflect;

/**
 * An annotation used to declare dependencies on other types and members on the classpath.
 * <p>
 * This is handy in the case that you want to use gwt reflection on a type that you
 * cannot annotate without maintaining duplicate copies on your own classpath (ick!).
 * <p>
 * Simply add this annotation to your @GwtRetention declaration,
 * then whenever that type is included with {@link GwtReflect#magicClass(Class)},
 * then so will the declared members.
 * <p>
 * This will allow you to use Class.forName(""), and simply catch the missing
 * class exception if the type actually isn't on the classpath, <b>provided
 * you set {@link #optional()} to true</b>.  By default, a missing dependency
 * on the gwt module classpath will cause the compiler to throw an error.
 * <p>
 * This can be used to perform "feature sniffing" on the gwt classpath.
 *
 * @author james.nelson
 *
 */
@Documented
@Target({})// only usable inside other annotations
@Retention(RetentionPolicy.CLASS)
public @interface Dependency {

  /**
   * An annotation used to filter specific members out of the class(es) specified
   * by an {@link Dependency} annotation.
   * <p>
   * This can be used if you need to declare retention on types you cannot
   * actually annotate yourself; use @Retention to declare the types to keep,
   * and @Member to refine the retention to particular members (methods / ctors probably).
   * <p>
   * Using @Retention without @Member filters will cause an entire type to be retained.
   *
   * @author james.nelson
   *
   */
  @Documented
  @Retention(RetentionPolicy.CLASS)
  @Target({})// only usable inside other annotations
  @interface Member {
    String name() default "<init>";// this is the method name of a constructor
    Class<?>[] parameters() default {};// we are defaulted to zero-arg constructor
    String[] jsniParameters() default {};// we are defaulted to zero-arg constructor
    int privacy() default PUBLIC|PROTECTED|PRIVATE;// you probably shouldn't change this
  }

  /**
   * A package name to retain.
   * <p>
   * This value is optional, and it's retention level can be further
   * restricted to a single class, by use of the {@link #className()} method.
   * <p>
   * If className is not a fully qualified name,
   * packageName() cannot be left empty.
   *
   * @return - A package to keep.
   */
  String packageName() default "";
  /**
   * A specific class to retain.
   * <p>
   * This value is optional, and it may either contain a fully qualified class name
   * (Class.getCanonicalName()), or used in conjunction with packageName() to
   * form a fully qualified name.
   * <p>
   * The annotation processor using this annotation will have to do some classpath guessing
   * in order to find the type you wish to keep.
   *
   * @return
   */
  String className() default "";

  /**
   * A specific array of classes to retain.
   * <p>
   * This value is strictly optional, and does not have to match the
   * package name specified (if any).
   * <p>
   * If none of {@link #packageName()}, {@link #className()} or {@link #classes()}
   * are specified, the annotation processor may emit a warning or throw an error.
   *
   * @return - An array of classes to keep.
   */
  Class<?>[] classes() default {};

  /**
   * A filter to reduce the members retained.
   * <p>
   * The default value of an empty array equates to "apply to all".
   * <p>
   * If more than one class is specified, the filter applies to them all.
   *
   * @return - an array of annotations describing what members the
   * dependency applies to.
   */
  Member[] members() default {};

  /**
   * Whether or not the declared dependency is optional.
   * <p>
   * Default is false, which will tell the compile to break if the dependency is missing.
   *
   * @return
   */
  boolean optional() default false;


}
