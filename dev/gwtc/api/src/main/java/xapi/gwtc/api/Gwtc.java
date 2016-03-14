package xapi.gwtc.api;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Resource;
import xapi.annotation.compile.Resource.ResourceType;
import xapi.annotation.reflect.MirroredAnnotation;
import xapi.annotation.ui.UiTemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to describe gwt compiler settings.
 * <p>
 * This annotation may be applied to a package, class or method,
 * though support for these locations is not guaranteed by all implementations.
 * <p>
 * The primary use case for this is in testing; when using JUnit 4 support,
 * there is no simple means to define which GWT modules or sources to inherit,
 * so this annotation is used to fill in those details.
 * <p>
 * By putting this annotation at a package level, you can
 *
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation
public @interface Gwtc {

  /**
   * This enum describes what kind of gwt compile is to be preferred.
   * <p>
   * When selecting the mode, the compiler will start at the method or class
   * which is to be compiled, and search up the class hierarchy for a {@link Gwtc}
   * annotation that has set the {@link Gwtc#compileMode()} to anything other
   * than {@link CompileMode#INHERIT}.
   * <p>
   * The search order of inherited {@link Gwtc#compileMode()} will be based
   * upon the first {@link Gwtc#inheritanceMode()} value found.
   *
   * @author "James X. Nelson (james@wetheinter.net)"
   *
   */
  enum CompileMode {
    /**
     * Run a standard full GWT compile.
     */
    GWTC,
    /**
     * Run in a superdevmode shell.  Default value.
     */
    SUPERDEV,
    /**
     * Run in standard dev mode; not yet supported
     */
    DEV,
    /**
     * Run GWTTestCase in production mode; not yet supported
     */
    JUNIT3_PROD,
    /**
     * Run GWTTestCase in dev mode; not yet supported
     */
    JUNIT3_DEV,
    /**
     * Run a full compile on the annotated method or class,
     * without including any other entry points or dependencies.
     * <p>
     * Useful for test classes which are testing full GWT compiles in production mode.
     */
    GWTC_ISOLATED,
    /**
     * Run a full compile on the annotated method or class,
     * without including any other entry points or dependencies.
     * <p>
     * Useful for tests which want an isolated module definition,
     * to ensure that no other settings / source inclusions interfere
     * with the compile.
     */
    SUPERDEV_ISOLATED,
    INHERIT
  }

  enum IsolationMode {
    MONOLITHIC,
    PER_PACKAGE,
    PER_CLASS,
    PER_METHOD
  }

  /**
   * This enum is used to control how ancestor {@link Gwtc} annotation
   * are used to build generated gwt.xml.
   *
   * @author "James X. Nelson (james@wetheinter.net)"
   *
   */
  enum AncestorMode {
    INHERIT_ONE_PARENT,
    INHERIT_ALL_PARENTS,
    INHERIT_ENCLOSING_CLASSES,
    INHERIT_SUPER_CLASSES,
    INHERIT_CHILDREN
    // TODO INHERIT_INTERFACES
  }

  /**
   * @return the preferred {@link CompileMode} for this compile;
   * this value may be overridden, where the annotation closest
   * to the method / class being run will determine the CompileMode used.
   * <p>
   * The default run mode is inherit, which will cause the compiler to
   * check encolsing classes or packages for the compile mode to use.
   *
   * If no parent specifies any mode, {@link CompileMode#SUPERDEV} will be chosen
   * to encourage maximum development time compile speed.
   */
  CompileMode compileMode() default CompileMode.INHERIT;

  /**
   * @return an array of {@link Property} annotations to describe
   * System.setProperies() calls to make.
   */
  Property[] propertiesSystem() default {};
  /**
   * @return an array of {@link Property} annotations to describe
   * &lt;set-property> elements to generate.
   */
  Property[] propertiesGwt() default {};
  /**
   * @return an array of {@link Property} annotations to describe
   * &lt;set-configuration-property> elements to generate.
   */
  Property[] propertiesGwtConfiguration() default {};

  GwtcProperties[] propertiesLaunch() default {};

  /**
   * @return an array of {@link Resource} annotations describing additional
   * gwt.xml to include.  If you want to manually add snippets of xml, be sure
   * to set {@link Resource#type()} to {@link Resource.ResourceType#LITERAL_VALUE}.
   * <p>
   * The .gwt.xml extension will be added automatically when looking for inherited gwt.xml.
   * <p>
   * Supported resource types are:
   * <ul><li>
   * {@link ResourceType#CLASSPATH_RESOURCE} - Searches classloader</li><li>
   * {@link ResourceType#LITERAL_VALUE} - Adds generated snippet of xml</li><li>
   * {@link ResourceType#ABSOLUTE_FILE} - Specify an absolute path, include extension</li><li>
   * {@link ResourceType#CLASS_NAME} - Specifies a classname to search for {@link Gwtc} annotations</li><li>
   * {@link ResourceType#PACKAGE_NAME} - Specifies a packagename to search for {@link Gwtc} annotations</li></ul>
   *
   */
  Resource[] includeGwtXml() default {
    @Resource("com.google.gwt.core.Core")
  };

  String[] includeSource() default { "client" };

  IsolationMode isolationMode() default IsolationMode.MONOLITHIC;

  /**
   * @return an array of {@link Resource} annotations describing additional
   * html code to include in generated host page.  If you want to manually
   * add snippets of html, be sure to set {@link Resource#type()}
   * to {@link Resource.ResourceType#LITERAL_VALUE}.
   * <p>
   * Inheriting an entire file with .htm(l) suffix will cause the <pre><head> and <body></pre> elements
   * to be extracted from that file and appended to the generated Html file.  All other
   * classpath resources will be treated as snippets to directly embed in the body.
   * <p>
   * Set {@link Resource#qualifier()} to "head" to include values in the head instead of body.
   * <p>
   * Supported resource types are:
   * <ul><li>
   * {@link ResourceType#CLASSPATH_RESOURCE} - Searches classloader</li><li>
   * {@link ResourceType#LITERAL_VALUE} - Adds generated snippet of html</li><li>
   * {@link ResourceType#ABSOLUTE_FILE} - Specify an absolute path, include extension</li><li>
   *
   */
  UiTemplate[] includeHostHtml() default {};

  /**
   * @return an array of {@link Dependency} elements to include.
   * <p>
   * These describe your classpath dependencies, and not your gwt.xml module inheritance.
   * For most cases, you should not have to set this, though using maven type
   * dependencies can allow you to easily include jars that are on the classpath.
   * <p>
   * The compiler will look up the META-INF/maven/group-id/artifact-id/pom.xml resource,
   * and add the jar / folder containing said dependency to the classpath.
   * <p>
   * If this fails, the compiler will emit a warning and then use
   * System.getenv("M2_HOME")/group/id/artifact-id/version/artifact-id-version.jar
   */
  Dependency[] dependencies() default {
    @Dependency(Dependency.DIR_BIN),
    @Dependency(Dependency.DIR_TEMP),
    @Dependency(Dependency.DIR_GEN)
  };

  /**
   * @return an array of {@link AncestorMode} values to describe how the given
   * {@link Gwtc} annotation should inherit settings from enclosing classes and packages.
   * <p>
   * This is primarily to simplify sharing inheritance settings, while also
   * allowing classes or methods to ignore enclosing class or package level settings.
   */
  AncestorMode[] inheritanceMode() default {
    AncestorMode.INHERIT_ALL_PARENTS, AncestorMode.INHERIT_SUPER_CLASSES, AncestorMode.INHERIT_ENCLOSING_CLASSES
  };
  /**
   * return true to print generated gwt.xml to stdOut during compilation
   */
  boolean debug() default false;
}
