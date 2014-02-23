package xapi.annotation.compile;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to describe a dependency in a compile.
 * <p>
 * {@link #value()} is the main descriptor used for the dependency;
 * for {@link DependencyType#RELATIVE} and {@link DependencyType#ABSOLUTE},
 * artifactId() is where you will specify the filename you wish to locate.
 * <p>
 * When using {@link DependencyType#MAVEN}, you will specify all three fields
 * as a normal dependency, {@link #value()} is the artifactId,
 * while {@link #groupId()} and {@link #version()} are exactly what you expect.
 * <p>
 * Any property ${keys} found in any of the annotation values will be replaced
 * with whatever {@link xapi.util.X_Properties#getProperty(String, String)} returns
 * (if there is no property set, you will get "keys" as the value for ${keys}).
 * The PropertiesService defaults to using System.properties, though you are free
 * to inject your own property provider.
 * <p>
 * Note that {@link DependencyType#RELATIVE} will check {@link #groupId()} for a value
 * to use as the base of the relative uri; special values like {@link Dependency#DIR_TEMP},
 * {@link Dependency#DIR_BIN}, {@link Dependency#DIR_GEN} or {@link Dependency#DIR_LAUNCH} 
 * can be used as {@link #groupId()} to specify where the resource must be found.
 * <br/>
 * Providing no groupId() when using a {@link DependencyType#RELATIVE} Dependency
 * will case all of the special dirs to be searched in the following order:
 * <ul><li>
 * bin</li><li>
 * temp</li><li>
 * gen</li><li>
 * launch</li></ul>
 * <p>
 * Note that changing the {@link #version()} will cause the default compilers
 * to discard the classloader / existing runtime environment.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target({})// May only be used as a value in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependency {

  /**
   * The temporary directory; default is OS-temp directory (/tmp)
   */
  String DIR_TEMP = "${dir.temp}";
  /**
   * The directory in which the current process was launched, new File(".")
   */
  String DIR_LAUNCH = "${dir.cwd}";
  /**
   * The bin directory; whatever directory we can ascertain that the {@link Dependency}
   * was launched from.  
   * try {
   *   return getClassloader().findResource(toClassLocation(Dependency.class.getName()));
   * } catch (Exception e) {
   *   return Dependency.class.getProtectionDomain().getCodesource().getLocation();
   * }
   */
  String DIR_BIN = "${dir.bin}";
  /**
   * The bin directory; whatever directory we can ascertain that the {@link Dependency}
   * was launched from.  
   * try {
   *   return getClassloader().findResource(toJavaLocation(Dependency.class.getName()));
   *   // toJavaLocation = name.replace(packageName, packageName.replace('.', '/')
   *   // + getSimpleName.split("$")[0] + ".java"
   * } catch (Exception e) {
   *   return Dependency.class.getProtectionDomain().getCodesource().getLocation();
   * }
   */
  String DIR_SOURCE = "${dir.src}";
  /**
   * The gen directory; must be set as a system property (or in whatever PropertyService
   * is used by X_Properties).  Some compilers, like the gwt compiler, will fill
   * these values for you.
   */
  String DIR_GEN = "${dir.gen}";
  /**
   * The gen directory; must be set as a system property (or in whatever PropertyService
   * is used by X_Properties).  Some implementations, like the maven launcher,
   * will fill this value in for you.  The gwt compiler will default this
   * special location to the WAR/moduleName directory.
   */
  String DIR_TARGET = "${dir.target}";
  /**
   * The war directory; must be set as a system property (or in whatever PropertyService
   * is used by X_Properties).  Some implementations, like the gwt compiler,
   * will fill this value in for you.  
   */
  String DIR_WAR = "${dir.target}";
  
  String value();
  String groupId() default "";
  String version() default "";
  String classifier() default "";
  DependencyType dependencyType() default DependencyType.RELATIVE;
  
  public enum DependencyType {
    RELATIVE, ABSOLUTE, MAVEN
  }
  
}
