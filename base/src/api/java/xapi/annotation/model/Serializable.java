package xapi.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Marking fields or methods as serializable will cause them to be serialized upon HTTP requests.
 *
 * This annotation allows control over whether a value may move from server to client, or vice versa.
 * This is useful to prevent leaking sensitive data to clients, or to prevent sending useless data to server.
 *
 * By default, all non-android JRE environments are considered servers.
 * See {@link xapi.prop.X_Properties#isServer} for details on determining if a runtime is a server or not.
 *
 * If you wish to make a non-serializable field;
 * set both {@link #clientToServer()} and {@link #serverToClient()} to false;
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Serializable {

  /**
   * @return true to obfuscate values before serialization.
   *
   * The default obfuscator will use a one-time pad to hash your values.
   */
  public boolean obfuscated() default false;

  ClientToServer clientToServer() default @ClientToServer;

  ServerToClient serverToClient() default @ServerToClient;

  boolean value() default true;
}
