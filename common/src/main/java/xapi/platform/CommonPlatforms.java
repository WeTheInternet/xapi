package xapi.platform;

/**
 * A set of common platforms that we expect to use; these will (should) be
 * setup in the global scope to help runtimes prefer a given implementation,
 * in cases where the classpath might expose multiple choices at once.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/13/17.
 */
public enum CommonPlatforms {
    Jre, Gwt, Javafx, Vertx, Servlet, Jaxrs, Maven, Gradle, Android, Ios
}
