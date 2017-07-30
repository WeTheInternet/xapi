package xapi.scope.api;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Import;
import xapi.annotation.compile.SourceRewrite;
import xapi.annotation.reflect.MirroredAnnotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
@Documented
@MirroredAnnotation
@Retention(RUNTIME)
@Target({PACKAGE, TYPE, ANNOTATION_TYPE})
public @interface XApi {

  /**
   * Specify imports to have the XApi settings of this class applied to arbitrary other classes.
   *
   * This is useful to apply tooling to classes you do not control.
   */
  Import[] imports() default {};

  /**
   * Specify artifact ids of dependencies; not terribly useful until a project generator is built.
   *
   * In theory, a compiler plugin could analyze dependencies and include them in generated sources,
   * then create a jar-slimmer plugin which collects only the used dependencies.
   */
  Dependency[] dependencies() default {};

  /**
   * Whether or not to perform field injection of the given type.
   */
  boolean performInjection() default true;

  /**
   * A bag of properties for you to pass around to plugins.
   *
   * Do try to prefix your properaties with something other than xapi.*
   */
  Property[] properties() default {};

  /**
   * The scope in which this class should be run.
   *
   * Determines which scope is used for both compile time and runtime operations.
   * By initializing a scope before an object is injected / created,
   * you can control what services or objects are created within this class.
   *
   * Injections will be provided from the running / created scope matching this class.
   */
  Class<? extends Scope> scope() default Scope.class;

  /**
   * Whether or not this class should be considered An entry point.
   *
   * An entry point must have all of it's dependencies resolved,
   * and (in the future), control flow analysis should be done from the entry point,
   * with all opportunities for compiler plugins to process and re-process generated code.
   *
   */
  boolean entryPoint() default false;

  /**
   * A collection of source rewrites to apply.
   *
   * Not implemented yet, but this will provide the means to load and rewrite arbitrary source code.
   * For example, if a library has protected methods that you need to be public,
   * it should be simple to specify a method, class or package, and twiddle its modifier.
   *
   */
  SourceRewrite[] rewrites() default {};

  /**
   * If finalBuild is true, all source code in all dependencies will be fully processed by all registered plugins.
   *
   * This amounts to a "dist" build.
   *
   * You should only use this right before a final compile to production,
   * and regularly in your test code (generate your modules in test code so you can look at them).
   */
  boolean finalBuild() default false;

  /**
   * @return an array of strings to be treated as XApi source.
   *
   * The semantics of how this source will be applied will depend on where the
   * @Xapi annotation is used, and which processor is inspecting the annotation.
   *
   */
  String[] value() default {};

}
