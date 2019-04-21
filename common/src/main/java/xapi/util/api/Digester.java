/**
 *
 */
package xapi.util.api;

/**
 * A simple interface for creating digests of bytes; this is used primarily so we can have
 * shared code that is able to invoke Digest operations, without accessing any potentially
 * unsupported classes in a given environment.
 * <p>
 * This also allows the digest function to be easily pluggable, so end users can select the
 * digest algorithm to use.  The default implementation uses java core MD5 hashing.
 * <p>
 * This interface should be instantiated via X_Inject.instance(Digester.class),
 * as a digester needs to store internal state, it cannot be a singleton.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface Digester {

  Digester update(byte[] bytes, int offset, int length);

  byte[] digest();

  byte[] digest(byte[] bytes);

  String toString(byte[] bytes);
}
