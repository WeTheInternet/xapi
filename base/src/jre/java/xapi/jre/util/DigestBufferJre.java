/**
 *
 */
package xapi.jre.util;

import java.nio.ByteBuffer;

import xapi.annotation.inject.InstanceDefault;

/**
 * The default {@link DigestBuffer} implementation; uses java core MD5 hashing.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@InstanceDefault(implFor = DigestBuffer.class)
public class DigestBufferJre extends DigesterJre implements DigestBuffer {

  /**
   * @see DigestBuffer#update(java.nio.ByteBuffer)
   */
  @Override
  public DigestBuffer update(final ByteBuffer buffer) {
    digest.update(buffer);
    return this;
  }

  /**
   * @see DigestBuffer#digest(java.nio.ByteBuffer)
   */
  @Override
  public byte[] digest(final ByteBuffer buffer) {
    update(buffer);
    return digest.digest();
  }

}
