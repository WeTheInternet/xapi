package xapi.source;

import java.io.IOException;

import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;

public class X_Base64 {

  private X_Base64(){}

  private static final int MIN = '+';
  private static final int MAX = 'z';
  private static final int[] charToIntMap = new int[MAX+1];
  private static final char[] intToCharMap = new char[64];
  
  static {
    char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    for (int i = chars.length; i --> 0; ) {
      char ch = chars[i];
      intToCharMap[i] = ch;
      charToIntMap[ch] = i;
    }
  }
  
  public static char toBase64(int number) {
    assert 0 < number : "Base64 number "+number+" cannot be negative";
    assert 64 > number : "Base64 number "+number+" must be between 0 and 63 (inclusive)";
    return intToCharMap[number];
  }
  
  public static int fromBase64(char number) {
    assert MIN <= number : "Base64 char "+number+" must be above minimum value, "+(char)MIN;
    assert MAX >= number : "Base64 char "+number+" must be below maximum value, "+(char)MAX;
    assert 'A' == number || charToIntMap[number] > 0 : "Base64 char "+number+" ("+(int)number+ ") is not a valid Base64 value.";
    return charToIntMap[number];
  }
  /*
  VLQ encoding copied directly from Google's closure-compiler, Base64VLQ.java
  https://code.google.com/p/closure-compiler/source/browse/src/com/google/debugging/sourcemap/Base64VLQ.java
  */

  // A Base64 VLQ digit can represent 5 bits, so it is base-32.
  private static final int VLQ_BASE_SHIFT = 5;
  private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT;

  // A mask of bits for a VLQ digit (11111), 31 decimal.
  private static final int VLQ_BASE_MASK = VLQ_BASE-1;

  // The continuation bit is the 6th bit.
  private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

  /**
   * Converts from a two-complement value to a value where the sign bit is
   * is placed in the least significant bit.  For example, as decimals:
   *   1 becomes 2 (10 binary), -1 becomes 3 (11 binary)
   *   2 becomes 4 (100 binary), -2 becomes 5 (101 binary)
   */
  private static int toVLQSigned(int value) {
    if (value < 0) {
      return ((-value) << 1) + 1;
    } else {
      return (value << 1) + 0;
    }
  }

  /**
   * Converts to a two-complement value from a value where the sign bit is
   * is placed in the least significant bit.  For example, as decimals:
   *   2 (10 binary) becomes 1, 3 (11 binary) becomes -1
   *   4 (100 binary) becomes 2, 5 (101 binary) becomes -2
   */
  private static int fromVLQSigned(int value) {
    boolean negate = (value & 1) == 1;
    value = value >> 1;
    return negate ? -value : value;
  }

  /**
   * Writes a VLQ encoded value to the provide appendable.
   * @throws IOException
   */
  public static void encodeVLQ(Appendable out, int value) throws IOException {
    value = toVLQSigned(value);
    do {
      int digit = value & VLQ_BASE_MASK;
      value >>>= VLQ_BASE_SHIFT;
      if (value > 0) {
        digit |= VLQ_CONTINUATION_BIT;
      }
      out.append(toBase64(digit));
    } while (value > 0);
  }

  /**
   * Decodes the next VLQValue from the provided CharIterator.
   */
  public static int decode(CharIterator in) {
    int result = 0;
    boolean continuation;
    int shift = 0;
    do {
      char c = in.next();
      int digit = fromBase64(c);
      continuation = (digit & VLQ_CONTINUATION_BIT) != 0;
      digit &= VLQ_BASE_MASK;
      result = result + (digit << shift);
      shift = shift + VLQ_BASE_SHIFT;
    } while (continuation);

    return fromVLQSigned(result);
  }
  
  public static CharIterator toCharIterator(String value) {
    return new StringCharIterator(value);
  }
}
