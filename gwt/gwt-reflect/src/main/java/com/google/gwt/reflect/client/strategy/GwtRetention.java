package com.google.gwt.reflect.client.strategy;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.reflect.client.GwtReflect;

/**
 * Use this annotations on your types and members to refine your
 * runtime reflection retention strategy.
 * <p>
 * This annotation allows you to do two things:<br>
 * {@link #asyncOnly()} tells the gwt compiler that the annotated
 * type or member should only be loaded in the classloader-only split point.<br>
 * {@link #dependencies()} tells the compiler to include external dependencies
 * whenever the given type or member is included.
 * <p>
 * This gives you the power to defer code bloat into one big ball of code
 * that will not affect your application bootsrap timing.  It also allows you
 * to declare reflection retention on classes you do not control,
 * so you don't have to have an unmaintainable mess of copies of source code
 * just to add your reflection retention annotations.
 * <p>
 * When you annotate a member (method, field or constructor), it's dependencies
 * will be added when that member is included (currently when the class is loaded,
 * but work is being done for more fine-grained inclusion).
 * If the member is deferred to the com.google.gwt.reflect.client.ConstPool split point,
 * so are its dependencies.  If loaded synchronously ({@link #asyncOnly()}=false),
 * it is included when the enclosing type is sent through {@link GwtReflect#magicClass(Class)},
 * or when you directly access that member as a CONSTANT-ONLY reference:
 * <br>
 * SomeClass.class.getField("constantName");// includes dependencies on that field
 * SomeClass.class.getMethod("constantName", CONSTANT_CLASS_ARRAY);// array must be a constant field.
 * SomeClass.class.getDeclaredMethods();// includes all dependencies of all declared methods.
 * <p>
 * Support for the full level of declared optimizations is not yet
 * complete, but this annotation will be used to define more
 * complex strategies for retaining reflective access to runtime types and members.
 * <p>
 * With great power comes great responsibility: Misuse of Retention policies may
 * lead to code bloat, and cause your application more code than is necessary
 * to perform its given duty.
 * <p>
 * That said, this does give you the power to directly control what code gets
 * loaded where, so if you know you want to include a given snippet of
 * code in a given code split, this will allow you to specify members you
 * want to include now, but call later on.
 * <p>
 * If you need to pull code of non-exclusive or leftover code blocks,
 * this annotation will allow you to pull specific code into an exclusive
 * code block, and reduce clinits() in your non-exclusive code blocks.
 *
 * @author james.nelson
 *
 */
@Documented
@Target({CONSTRUCTOR, METHOD, FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface GwtRetention {

  final int 
    PUBLIC = 1,
    PRIVATE = 2,
    PROTECTED = 4,
    PACKAGE = 8,
    ALL = PRIVATE | PROTECTED | PUBLIC | PACKAGE;

  /**
   * An int register of the privacy level of members to keep.
   * <p>
   * Use this to filter out unneeded members and reduce code size.<br>
   * Set to 0 to elide members.
   * <p>
   * This will allow you to do strange things, like only keep public and private;
   * really, whatever filter you want to reduce your compile size (just be careful!)
   *
   */
  int privacy() default ALL;

  /**
   * This element will only be included in the monolithic ClassLoader.
   * <p>
   * This will allow you to defer things you "want later", and only
   * include what you need now behind a call to
   * {@link GwtReflect#getClassloader(com.google.gwt.user.client.rpc.AsyncCallback)}.
   * <p>
   * Future work is planned to build a preprocessor which checks against unused reflection
   * objects, and pulls those references out of the main application, and into the classloader.
   * <p>
   * In anticipation of this optimization, do try to prefer the "most constant possible"
   * manner of retrieving your reflection data.
   * <p>
   * BEST:  SomeClass.class.getMethod("someConstantString", String.class, int[].class);<br/>
   * GOOD: SomeClass.class.getMethod(SOME_CONSTANT_STRING, SOME_CONSTANT_CLASS_ARRAY);<br/>
   * OKAY: Class c = GwtReflect.magicClass(SomeClass.class); c.getMethod(...); <br/>
   * BAD: Method[] methods = GwtReflect.magicClass(SomeClass.class).getMethods();<br/>
   * WORST: for (Method method : GwtReflect.magicClass(SomeClass.class).getMethods())<br/>
   * if (isMethodIWant(method)) doSomething(method);
   * <p>
   * In order for this to work right, we will have to scan your ast for all calls
   * into reflection methods, and pull out anything unused into the classloader code block.
   * If we are unable to determine a minimum bounds for data needed in a class,
   * we will have to include it all immediately.
   * <p>
   * Any element which explicitly uses classLoaderOnly() will always be deferred,
   * and you should get in the habit of explicitly declaring this,
   * as it will make scanning either easier, or entirely unnecessary.
   * <p>
   * Once the deferment optimization is complete, a maven mojo will be provided
   * that can simply scan your gwt module, ignore all existing @GwtRetention annotations,
   * and auto annotate all elements with the most optimal retention levels possible.
   *
   * @return true to defer the annotation method, field, constructor or parameter
   */
  boolean asyncOnly() default false;

  /**
   * This option is only valid when {@link GwtRetention} is placed inside
   * a class or package level {@link ReflectionStrategy}.
   * <p>
   * It tells the compiler whether or not to search super types for
   * public members of the given member type.
   * <p>
   * This defaults to true so out-of-the-box behavior matches
   * that of JVM reflection. You may want to set it to false to reduce code size;
   * just remember to always access members by their declaring class),
   */
  boolean keepPublicSuperMembers() default true;
  
  /**
   * If you want to keep your parameter annotations, you must set this member to true
   * <p>
   * If it is a common request to keep complete jvm parity,
   * this default might change, so there is no harm in explicitly setting it 
   * to false when you are trimming code bloat.
   * 
   */
  int annotationRetention() default ReflectionStrategy.RUNTIME;

  /**
   * If you want to keep your parameter annotations, you must set this member to true
   * <p>
   * If it is a common request to keep complete jvm parity,
   * this default might change, so there is no harm in explicitly setting it 
   * to false when you are trimming code bloat.
   * 
   */
  boolean keepParameterAnnotations() default false;
  
  /**
   * Any additional dependencies to declare?
   * <p>
   * Add them to a GwtRetention annotation, and instead of explicitly
   * overriding and maintaining a copy of classes you don't control,
   * just add @Dependencies to a type you do control, and use it
   * to declare when and where you want to include the given dependency.
   * <p>
   * Be sure to read the doc on {@link GwtRetention} to see the
   * best practices for using this somewhat dangerous tool.
   * <p>
   * If you are using complex retention strategies,
   * you should run emma (and story-of-your-compile) to check your code coverage
   * and see if you can refine your strategy to keep code bloat down.
   * <p>
   *
   *
   * @return - An array of descriptors of other types to keep along with the
   * annotated retention member.
   */
  Dependency[] dependencies() default {};

}
