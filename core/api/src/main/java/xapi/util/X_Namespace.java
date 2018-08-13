package xapi.util;

import xapi.util.service.PropertyService;


/**
 * A collection of string constants, used as property keys throughout the app.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class X_Namespace {
  private X_Namespace() {}//static only.

  /**
   * The current version of XApi, updated after releases;
   * this could probably be derived from poms in jars on classpath,
   * but it's faster at runtime to use a constant field.
   *
   */
  public static final String XAPI_VERSION = "0.5.1-SNAPSHOT";

  /**
   * Latest published version of xapi.
   */
  public static final String XAPI_RELEASE_VERSION = "0.5";

  public static final String GWT_VERSION = "2.8.0";

  /**
   * The groupId used for the xapi project.  This is used to
   * allow anyone that wishes to fork the project to use their own groupId.
   *
   * It also allows tracing all source-level accessors of the maven group id.
   */
  public static final String XAPI_GROUP_ID = "net.wetheinter";

  /**
   * META-INF - Used a separate constant to encourage fragment reuse.
   */
  public static final String META_INF = "META-INF";
  /**
   * META-INF/singletons
   * Default folder for location of singleton object mapping;
   */
  public static final String DEFAULT_SINGLETONS_LOCATION = META_INF+"/singletons";
  /**
   * META-INF/instances
   * Default folder for location of instance object mapping;
   */
  public static final String DEFAULT_INSTANCES_LOCATION = META_INF+"/instances";
  /**
   * assets/wti/singeltons
   * Default android folder for location of singleton object mapping;
   */
  public static final String ANDROID_SINGLETONS_LOCATION = "assets/xapi/singletons";
  /**
   * assets/wti/instances
   * Default android folder for location of instance object mapping;
   */
  public static final String ANDROID_INSTANCES_LOCATION = "assets/xapi/instances";
  /**
   * "xapi.platform" System property for specifying injection runtime.
   *
   * For runtime injection in a jre environment
   */
  public static final String PROPERTY_PLATFORM = "xapi.platform";

  /**
   * "xapi.debug" System property for setting debug level
   */
  public static final String PROPERTY_DEBUG = "xapi.debug";

  /**
   * "xapi.injector" System property for choosing the runtime injector to use.
   * Default value is xapi.jre.inject.RuntimeInjector
   */
  public static final String PROPERTY_INJECTOR = "xapi.injector";

  /**
   * "xapi.log.level" -> The default log level to use;
   * Choices are: ERROR, WARN, INFO, TRACE, DEBUG, SPAM or ALL.
   */
  public static final String PROPERTY_LOG_LEVEL = "xapi.log.level";

  /**
   * "xapi.model.root" Root class to use for model generation.
   * In gwt, defaults to xapi.gwt.model.ModelGwt.
   */
  public static final String PROPERTY_MODEL_ROOT = "xapi.model.root";
  /**
   * "xapi.model.strategy" The strategy to use for model generation;
   * passed to whatever xapi.dev.model.ModelGenerator is injected.
   */
  public static final String PROPERTY_MODEL_STRATEGY = "xapi.model.strategy";
  /**
   * "xapi.properties" System property for setting the class to act as
   * runtime {@link PropertyService}. Default implementation backed by System.properties
   */
  public static final String PROPERTY_PROVIDER = "xapi.properties";
  /**
   * "xapi.singletons" System property for looking up the singletons location to use at runtime
   */
  public static final String PROPERTY_SINGLETONS = "xapi.singletons";

  /**
   * "xapi.instances" System property for looking up the instances location to use at runtime
   */
  public static final String PROPERTY_INSTANCES = "xapi.instances";

  /**
   * "xapi.multithreaded"
   * System property for enabling or disabling the use of mutlithreading.
   * Any non-null value will enable multithreading;
   * send any integer to limit the maximum amount of threads to use.
   */
  public static final String PROPERTY_MULTITHREADED = "xapi.multithreaded";

  /**
   * "xapi.server"
   * System property for telling a runtime environment if it is a server or not.
   */
  public static final String PROPERTY_SERVER = "xapi.server";

  /**
   * "xapi.server.port"
   * System property for telling server what, if any, port to use.
   * Default is no port.
   */
  public static final String PROPERTY_SERVER_PORT = "xapi.server.port";

  /**
   * "xapi.server.host"
   * System property for telling server what hostname it is.
   * Default is localhost.
   */
  public static final String PROPERTY_SERVER_HOST = "xapi.server.host";

  /**
   * "xapi.inject.packages"
   * Comma-separated list of classpath prefixes to scan for runtime injection.
   *
   * Package names only; periods will be translated to / for resource lookup.
   */
  public static final String PROPERTY_RUNTIME_SCANPATH = "xapi.inject.packages";
  /**
   * "xapi.test" System property for enabling X_Runtime.isTest() to return true.
   * Set to anything other than false to have isTest() return true.
   * (and make a test profile for maven with this property set for you!)
   */
  public static final String PROPERTY_TEST = "xapi.test";

  /**
   * "xapi.meta"
   * Location to write injection metadata, if runtime injection is allowed
   */
  public static final String PROPERTY_RUNTIME_META = "xapi.meta";

  /**
   * "xapi.inject"
   * Send false to explicitly disable runtime injection.
   * Use system properties for jre environments,
   * and gwt module xml <set-property name="xapi.inject" value="false" />
   */
  public static String PROPERTY_USE_X_INJECT = "xapi.inject";

  /**
   * "xapi.home"
   * Sets the working location of a local xapi install, if any.
   * Default is ~/.xapi
   */
  public static String PROPERTY_XAPI_HOME = "xapi.home";

  /**
   * "xapi.mvn.repo"
   * The location of local maven repository to use.
   *
   * Default is ~/.m2/repository
   */
  public static String PROPERTY_MAVEN_REPO = "xapi.mvn.repo";

  /**
   * "xapi.mvn.repo.autocreate"
   * Whether or not to autocreate a maven repository if there isn't one.
   *
   * Default is TRUE.
   *
   * This is so desktop software can still run on any user's machine,
   * without requiring installation of maven in any way.
   */
  public static String PROPERTY_MAVEN_REPO_AUTOCREATE = "xapi.mvn.repo.autocreate";

  /**
   * "xapi.mvn.search.groups"
   * space-separated groupIds to use when searching for local workspace resolution of maven artifacts
   *
   * Default is "net.wetheinter de.mocra.cy"
   */
  public static String PROPERTY_MAVEN_SEARCH_GROUPS = "xapi.mvn.search.groups";

  /**
   * "xapi.mvn.workspace"
   * space-separated paths for local workspace projects that you wish to have
   * resolved from jars to output folders.
   *
   * Default is "/opt/xapi /opt/wti /opt/collide".
   *
   * Use "\ " to encode spaces in your paths (or, better yet, DON'T PUT SPACES IN PATHS).
   *
   * We may cheat a little and look at class file code sources to find xapi installs,
   * should the default location be missing.
   */
  public static String PROPERTY_MAVEN_WORKSPACE = "xapi.mvn.workspace";

  /**
   * "xapi.mvn.resolvable"
   *
   *  Default value is .*
   *
   *  A regex property which is applied to maven:artifact:coordinates.
   *  If the regex matches, the dependency will be resolved from a jar to local workspace.
   */
  public static String PROPERTY_MAVEN_RESOLVABLE = "xapi.mvn.resolvable";

  /**
   * "xapi.mvn.unresolvable"
   *
   *  Default value is .*uber.*
   *
   *  Set this property to empty string if you also wish to include uber modules,
   *  even though those modules should be used as a compiled artifact.
   *
   *  A regex property which is applied to maven:artifact:coordinates.
   *  If the regex matches, the dependency will be _not_ be resolved from a jar to local workspace.
   *
   *  A removal takes priority over an addition.
   */
  public static String PROPERTY_MAVEN_UNRESOLVABLE = "xapi.mvn.unresolvable";

  /**
   * If you are using the shared secrets from XapiSecrets to define a universal parent classloader,
   * you can overwrite this property to decide an artifact to load as the universal root.
   *
   * Default value if "net.wetheinter:xapi-dev-api:" + X_Namespace.XAPI_VERSION
   *
   * Any types included by this module should be considered sealed,
   * as they must be shared across all classworlds.
   */
  public static String PROPERTY_UNIVERSAL_COORDS = "xapi.universal.coord";


}
