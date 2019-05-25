/**
 *
 */
package xapi.jre.util;

import java.nio.ByteBuffer;

import xapi.util.api.Digester;

/**
 * Adds ByteBuffer support to the regular {@link Digester} interface.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface DigestBuffer extends Digester {

  DigestBuffer update(ByteBuffer buffer);

  byte[] digest(ByteBuffer buffer);

}
