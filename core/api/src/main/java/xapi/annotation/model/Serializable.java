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
 * See {@link xapi.util.X_Properties#isServer} for details on determining if a runtime is a server or not.
 *
 * Note that it is redundant to set both {@link #clientToServer()} and {@link #serverToClient()} to false;
 * if you do not include this annotation, your field will not be serializable at all.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Serializable {

  /**
   * @return true to allow the data to be sent from client to server.
   * false to prevent the value from leaving the client.
   *
   * Useful for dirty flags, or client-side rendering objects.
   */
  public boolean clientToServer() default true;

  /**
   *
   * @return true to obfuscate values before serialization.
   *
   * The default obfuscator will use a one-time pad to hash your values.
   */
  public boolean obfuscated() default false;

  /**
   *
   * @return true to allow the data to be sent from server to client.
   * false to prevent the value from leaving the server.
   *
   * Useful for passwords and other sensitive data.
   */
  public boolean serverToClient() default true;

}
