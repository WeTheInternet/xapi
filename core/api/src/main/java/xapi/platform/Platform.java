package xapi.platform;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.platform.PlatformSelector.AlwaysTrue;
import xapi.util.X_Namespace;


/**
 * This annotation is used to annotate other annotations which define a
 * particular injection platform.  This allows devs to create their own
 * custom injection targets to suit whatever java environment imaginable.
 *
 * The gwt injector searches classes annotated with the provider annotations,
 * {@link InstanceDefault}, {@link InstanceOverride},
 * {@link SingletonDefault} and {@link SingletonOverride}
 * and any annotations found annotated with @Platform will then make a
 * reflective call to the method isGwt().  If the method is missing or returns
 * false, that annotation is ignored.  If an annotation is found, a call is
 * then made to isDevMode(), which, if present, must match the expected
 * production mode status.
 *
 * The jre injector will also check any providers for platform annotations,
 * except it requires the annotations to provide an isJava() method, which must
 * return true.  The jre injector will also make a reflective call to .version(),
 * which, if it returns a double, will be matched to the current runtime
 * environment.  Any versions lower than current runtime will be ignored.
 *
 * This allows library writers to create java 7 or java 8 implementations,
 * but deliver java 6 (or even java 5) specific implementations.
 *
 * Java 5 and lower are not supported due to lack of annotation support.
 *
 *
 * Any platform annotation which supplies a runtimeSelector() method which
 * returns a Class<? extends PlatformSelector> will be able to arbitrarily
 * disable any type using a reflectively instantiated PlatformSelector.
 *
 * A gwt runtime will receive the StandardGeneratorContext object and TypeOracle
 * as parameters.
 *
 * A jre runtime will receive an instance of whatever class the main Injector
 * interface is bound to as its parameter.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Platform {

  /**
   * @return true if running in a real JRE; false for transpiled source
   */
  boolean isJava() default true;
  /**
   * @return true if running in any gwt runtime environment.
   * DevMode will return true for isJava, isGwt and isDevMode.
   */
  boolean isGwt() default false;
  /**
   * @return true if running on a server.  All clients ignore server types
   */
  boolean isServer() default false;
  /**
   * @return true if this target is mobile.
   */
  boolean isMobile() default false;
  /**
   * @return true if this is a debug platform; i.e., it is used in a test case,
   * or when you need to inject a debug implementation over top a production type.
   *
   * Debug types are allowed in any platform, and will only be used if the
   * property xapi.debug=true
   */
  boolean isDebug() default false;
  /**
   * @return true for platforms that are specifically for development purposes.
   * This differs from debug in that a dev mode platform has a "production enviro",
   * which is when a developer is using the tools.  A dev mode platform can still
   * have debug types injected for when developing the development tools. :)
   */
  boolean isDevMode() default false;
  
  /**
   * @return a class to use as a runtime platform selector.
   * Not really implemented yet.
   */
  Class<? extends PlatformSelector> selector() default AlwaysTrue.class;
  
  /**
   * @return a set of platforms to allow as fallback.
   */
  Class<? extends Annotation>[] fallback() default {};
  
  /**
   * @return - The folder relative to package root in which to place injection resources.
   * Most JREs will use META-INF/, android will use assets/, and projects with custom
   * packaging solutions can define their own location for injection manifests, 
   * provided they make sure to either tell the runtime injector which root to use,
   * (by setting property {@link X_Namespace#PROPERTY_SINGLETONS} and {@link X_Namespace#PROPERTY_INSTANCES})
   * or by defining their own runtime injector
   * (by setting property {@link X_Namespace#PROPERTY_INJECTOR},
   * 
   */
  String metaRoot () default "META-INF/";
}
