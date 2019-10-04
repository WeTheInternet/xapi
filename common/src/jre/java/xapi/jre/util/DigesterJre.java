/**
 *
 */
package xapi.jre.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import xapi.annotation.inject.InstanceDefault;
import xapi.util.X_Debug;
import xapi.util.api.Digester;

/**
 * The default {@link Digester} implementation; uses java core MD5 hashing.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@InstanceDefault(implFor = Digester.class)
public class DigesterJre implements Digester {

  protected final MessageDigest digest;

  public DigesterJre() {
    try {
      digest = MessageDigest.getInstance(getAlgorithm());
    } catch (final NoSuchAlgorithmException e) {
      throw X_Debug.rethrow(e);
    }

  }

  public DigesterJre(final MessageDigest instance) {
    digest = instance;
  }

  private String getAlgorithm() {
    return "MD5";
  }

  /**
   * @see xapi.util.api.Digester#update(byte[], int, int)
   */
  @Override
  public Digester update(final byte[] bytes, final int offset, final int length) {
    digest.update(bytes, offset, length);
    return this;
  }

  /**
   * @see xapi.util.api.Digester#digest()
   */
  @Override
  public byte[] digest() {
    return digest.digest();
  }

  /**
   * @see xapi.util.api.Digester#digest(byte[])
   */
  @Override
  public byte[] digest(final byte[] bytes) {
    return digest.digest(bytes);
  }

  /**
   * @see xapi.util.api.Digester#toHexString(byte[])
   */
  @Override
  public String toString(final byte[] bytes) {
    final StringBuilder uuidBuilder = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      uuidBuilder.append(Integer.toString((bytes[i] & 0xff)
                + 0x100, 36).substring(1).toLowerCase());
    }
    return uuidBuilder.toString();
  }

}
