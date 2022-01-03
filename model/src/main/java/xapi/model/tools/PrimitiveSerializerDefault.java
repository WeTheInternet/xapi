/**
 *
 */
package xapi.model.tools;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.lex.CharIterator;
import xapi.debug.X_Debug;
import xapi.util.X_Runtime;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@InstanceDefault(implFor=PrimitiveSerializer.class)
public class PrimitiveSerializerDefault implements PrimitiveSerializer {

  /**
   * The boundary which all negative ending numbers will be below.
   * A character below this bounadary is a termination digit that signifies the number is negative
   */
  private static final char NEGATIVE_VALUE_BOUNDARY = '>';

  /**
   * The boundary above which all continuation digits will occur.
   * And number below this value is a termination digit which signifies that the current number is complete.
   */
  private static final char END_VALUE_BOUNDARY = '_';

  /**
   * This continuation group of numbers is used to encode base 32 digits in a serialized number.
   * When serializing a number, the continuation bits are used to signify that there are still more
   * digits to serialize.  These are the top 33 printable ascii digits.
   * <p>
   * Note that there are 33 digits in this section and 32 in all others in order for us to handle
   * Integer.MIN_VALUE and Long.MIN_VALUE, both of which would normally overflow a positive value.
   * <p>
   * The information of whether a number is negative or positive is encoded in the final digit
   * (the first and only non-continuation digit), so in order to handle the fact that
   * Math.abs(MIN_VALUE) == Math.abs(MAX_VALUE) + 1, we allow the first continuation digit to
   * reach a value of 32 instead of 31, like all other base 32 digits.  Once we encounter the
   * final digit, we will negate the accumulated and current values to avoid negative integer overflows.
   * <p>
   * Also note that digits in this section are ordered according to their likely frequency in
   * English language text; this is to help improve GZipping of response bodies, as we are
   * far more likely to encounter the numbers 1 or 0 than 30 or 31.  The order chosen was
   * based upon http://en.wikipedia.org/wiki/Letter_frequency and other Google searches for
   * frequency of punctuation occurrence.
   *
   */
  private static final char[] CONTINUATION_NUM_SECTION = new char[] {
    'e', 't', 'a', 'o', 'i', 'n', 's', 'h', 'r', 'd',
    'l', 'c', 'u', 'm', 'w', 'f', 'g', 'y', 'p', 'b',
    'v', 'k', 'j', 'x', 'q', 'z', '_', '{', '}', '|',
    '~', '`',
  };

  /**
   * These numbers are used to denote the end of an encoded positive number.
   * See {@link #CONTINUATION_NUM_SECTION} for a more detailed breakdown of our
   * integer serialization policy.
   * <p>
   * Note that the values in this section are all strictly less than those in the
   * {@link #CONTINUATION_NUM_SECTION} and less than those of {@link #NEGATIVE_NUM_ENDING},
   * however, they are sorted by probabilistic frequency in English language text,
   * to aid in the optimization of the GZip protocol.
   */
  private static final char[] POSITIVE_NUM_ENDING = new char[] {
    'E', 'T', 'A', 'O', 'I', 'N', 'S', 'H', 'R', 'D',
    'L', 'C', 'U', 'M', 'W', 'F', 'G', 'Y', 'P', 'B',
    'V', 'K', 'J', 'X', 'Q', 'Z', '?', '@', '[',  ']',
    '^', '\\'
  };

  /**
   * The negative number endings encompass the sequentially lowest group of digits,
   * ordered in likely probability of occurrence in regular text Strings, to encourage
   * fewer unique digits in payload and help optimize GZip.
   * <p>
   * The lowest characters were chosen for negative values because negative numbers
   * will be less likely to occur than positive ones, so they are assigned the least
   * common characters (number digits and punctuation symbols).
   * <p>
   * Even then, number digits are prioritized so common values like -1 will result
   * in commonly encountered symbols in text.  -1 will be the space character instead
   * of the '1' character, as space is the most common symbol in written text.
   * <p>
   * Punctuation ordering loosely influenced by:
   * http://mdickens.me/typing/theory-of-letter-frequency.html
   * and the fact that we expect markdown symbols to be used more frequently.
   * <p>
   * See {@link #CONTINUATION_NUM_SECTION} for a detailed description of our Integer
   * serialization policies.
   *
   */
  private static final char[] NEGATIVE_NUM_ENDING = new char[] {
    // Note the first digit is '\0'; it is never used because we never have a -0 ending.
    // However, a value in the 0 position must be included for indexing to work correctly.
    // We never have a -0 due to how we pack numbers; a negative number's final digit
    // will always have a value of one or more; the only value capable of ending
    // in 0 is +0 itself.  So!  We use THAT slot to represent -1,
    // and when we do, we special case it to store minimal characters
   '\0', '1', '2', '3', '4', '5', '6', '7',  '8', '9',
    '0', '>', '.', ',', '-', '\'', '"', '/', '*', '(',
    ')',  ':', ';', '!', '+', '=', '#', '$', '%', '&',
    '<', '\t'
  };

  /**
   * See {@link #CONTINUATION_NUM_SECTION} for a detailed explanation of our integer serialization policy.
   * <p>
   * This lookup table is used to deserialize our base32 serialized integers by addressing this array
   * with the integer value of each character.  This is used to convert a serialized character back into the
   * base 32 number which sourced it.
   * <p>
   * This table is not ordered in increasing or decreasing order; rather we maintain three ranges of digits,
   * which are, from highest to lowest, {@link #CONTINUATION_NUM_SECTION}, {@link #POSITIVE_NUM_ENDING} and
   * {@link #NEGATIVE_NUM_ENDING}.  This is used so that we can deserialize a number without having to explicitly
   * encode its length.  Instead, we encode Continutation digits until we encounter a terminiation digit.
   * The range of the termination digit will be used to determine if the serialized number was positive or negative.
   * <p>
   * This serialization scheme was designed to be as GZip-friendly as possible, while also minimizing encoded
   * payload size.  It is also designed to be fast, as browsers like Chrome can serialize ascii char[] to string
   * much faster than they can handle UTF-8 encoded Strings.  (We use String.valueOf(char[]) as it skips any
   * UTF-8 encoding in GWT; we don't need it as we ensure all our serialized chars are < 127).
   */
  private static final int[] VALUE_TO_NUM
      = new int[] {

      0,  0,  0,  0,  0,   0,  0,  0,  0, 31,  // 0 - 10
   //                                     \t

      0,  0,  0,  0,  0,   0,  0,  0,  0,  0,  // 10 - 20
   //

      0,  0,  0,  0,  0,   0,  0,  0,  0,  0,  // 20 - 30
   //

      0,  0,  0, 23, 16,  26, 27, 28, 29, 15,  // 30 - 40
   //              !   "    #   $   %   &   '

      19, 20, 18, 24, 13,  14, 12, 17, 10,  1,  // 40 - 50
   //  (   )   *   +   ,    -   .   /   0   1

      2,  3,  4,  5,  6,   7,  8,  9, 21, 22,  // 50 - 60
   //  2   3   4   5   6    7   8   9   :   ;

      30, 25, 11, 26, 27,   2, 19, 11,  9,  0,  // 60 - 70
   //  <   =   >   ?   @    A   B   C   D   E

      15, 16,  7,  4, 22,  21, 10, 13,  5,  3,  // 70 - 80
   //  F   G   H   I   J    K   L   M   N   O

      18, 24,  8,  6,  1,  12, 20, 14, 23, 17,  // 80 - 90
   //  P   Q   R   S   T    U   V   W   X   Y

      25, 28, 31, 29, 30,  26, 31,  2, 19, 11,  // 90 - 100
   //  Z   [   \   ]   ^    _   `   a   b   c

      9,  0, 15, 16,  7,   4, 22, 21, 10, 13,  // 100 - 110
   //  d   e   f   g   h    i   j   k   l   m

      5,  3, 18, 24,  8,   6,  1, 12, 20, 14,  // 110 - 120
   //  n   o   p   q   r    s   t   u   v   w

      23, 17, 25, 27, 29,  28, 30,              // 120 - 130
   //  x   y   z   {   |    }   ~

  };
  private static final char NEG_ONE = '\0';

  public static void main(String ... a) {
    final int[] computed = computeValueToNum();
    System.out.println(Arrays.asList(computed));
  }

  @SuppressWarnings("unused")
  // We only use this method if we update any of the ordering of serialization char->int mappings
  private static int[] computeValueToNum(){
    final int[] VALUE_TO_NUM = new int[127];
    for (int i = VALUE_TO_NUM.length; i --> 0;) {
      VALUE_TO_NUM[i] = -1;
    }
    final char[] lookup = new char[127];
    for (int i = CONTINUATION_NUM_SECTION.length; i-->0; ) {
      assert VALUE_TO_NUM[CONTINUATION_NUM_SECTION[i]] == -1;
      VALUE_TO_NUM[CONTINUATION_NUM_SECTION[i]] = i;
      lookup[CONTINUATION_NUM_SECTION[i]] = CONTINUATION_NUM_SECTION[i];
    }
    for (int i = POSITIVE_NUM_ENDING.length; i-->0; ) {
      assert VALUE_TO_NUM[POSITIVE_NUM_ENDING[i]] == -1;
      VALUE_TO_NUM[POSITIVE_NUM_ENDING[i]] = i;
      lookup[POSITIVE_NUM_ENDING[i]] = POSITIVE_NUM_ENDING[i];
    }
    for (int i = NEGATIVE_NUM_ENDING.length; i-->0; ) {
      assert VALUE_TO_NUM[NEGATIVE_NUM_ENDING[i]] == -1;
      VALUE_TO_NUM[NEGATIVE_NUM_ENDING[i]] = i;
      lookup[NEGATIVE_NUM_ENDING[i]] = NEGATIVE_NUM_ENDING[i];
    }
    final StringBuilder b = new StringBuilder("= new int[] {\n"), l = new StringBuilder();
    b.append("\n   ");
    l.append("\n// ");
    for (int i = 0; i < VALUE_TO_NUM.length; i ++) {
      final int pos = VALUE_TO_NUM[i];
      final int val = (char)i;
      String num = pos == -1 ? " 0" : Integer.toString(pos);
      if (num.length() == 1) {
        num = " "+num;
      }
      if (pos == -1) {
        l.append("    ");
      } else if (val == '\t') {
        l.append(" \\t");
      } else if (val == ' ') {
          assert false : "Space characters no longer allowed in primitive serializer!";
          l.append("' ' ");
      } else {
        l.append(" "+((char)val)+"  ");
      }
      b.append(num).append(", ");
      if (i == VALUE_TO_NUM.length-1) {
        b.append("            ");
        i += 3;
      }
      if (i%10 == 9) {
        b.append(" // "+(i-9)+" - "+(i+1));
        b.append(l);
        l.setLength(0);
        l.append("\n// ");
        b.append("\n\n   ");
      }
      else if (i%5 == 4) {
        b.append(" ");
        l.append(" ");
      }
    }
    // Print out the value so we can hard-code it instead of compute it;
    // this method should be unused.
    System.out.println(b.append("};"));
    return VALUE_TO_NUM;
  }

  private static final int[] BIT_MASKS = new int[] {
    1, 2, 4, 8, 16, 32, 64, 128, 256
  };

  /**
   * This is a class that we will use so that we can determine the size of characters needed to
   * represent a number, while also collecting up the character we want for each position,
   * so that we can allocate a char[] of the correct size, without having to examine a number twice.
   * <p>
   * As we examine the base 32 length of a number, we collect the chars needed in this single-linked list.
   *
   * @author James X. Nelson (james@wetheinter.net, @james)
   *
   */
  protected static class CharacterBuffer {
    // The next buffer, if any
    protected CharacterBuffer next;
    // The index of the current slot; the head node will contain the total count so we can alloc a char[]
    protected int slot;
    // The char of the current node
    protected char c;

      public String serialize() {
          if (c == NEG_ONE) {
              return Character.toString(NEG_ONE);
          }
          // The very first buffer will have its slot set to size, since we know it will always exist and be in slot 0
          final char[] data = new char[slot];
          CharacterBuffer buffer = this;
          // Reset the sizing slot to an index of zero for our loop
          buffer.slot = 0;
          for (;buffer != null; buffer = buffer.next) {
              // Assemble the char[] computed as a linked list
              data[buffer.slot] = buffer.c;
          }
          return String.valueOf(data);
      }
  }

  /**
   * Consume characters from the supplied {@link CharIterator} to reassemble a serialized int value.
   * <p>
   * This will read in chars that are in the range of {@link #CONTINUATION_NUM_SECTION} as base 32
   * digits, until a termination digit from {@link #POSITIVE_NUM_ENDING} or {@link #NEGATIVE_NUM_ENDING}
   * are encountered, at which time deserialization will terminate, and the value will be returned.
   */
  @Override
  public int deserializeInt(final CharIterator i) {
    int value = 0, multi = 1;
    for (; i.hasNext();) {
      final char c = i.next();
      if (c == NEG_ONE) {
        assert value == 0 : "-1 should only occur at first char!";
        return Integer.MIN_VALUE;
      }
      final int delta = multi * VALUE_TO_NUM[c];
      assert delta >= 0 : "Unexpected Integer overlow; multi: " + multi + " ; char " + c  ;
      if (c < END_VALUE_BOUNDARY) {
        // We hit the end of this number
        if (c > NEGATIVE_VALUE_BOUNDARY) {
          // And the number was not negative; just return the sum
          return value + delta;
        }
        // Note that we negate the value and the delta, as this will prevent
        // an integer overflow of Integer.MIN_VALUE.
        return -value - delta;
      }
      // For continuation digits, just accumulate the sum of each base 32 digit.
      value += delta;
      multi <<= 5; // multiply by 32
    }
    assert false : "Malformed encoded number: `" + i + "` (outer ticks added)";
    return value;
  }

  /**
   * Consume characters from the supplied {@link CharIterator} to reassemble a serialized long value.
   * <p>
   * This will read in chars that are in the range of {@link #CONTINUATION_NUM_SECTION} as base 32
   * digits, until a termination digit from {@link #POSITIVE_NUM_ENDING} or {@link #NEGATIVE_NUM_ENDING}
   * are encountered, at which time deserialization will terminate, and the value will be returned.
   */
  @Override
    public long deserializeLong(final CharIterator l) {
      long value = 0, multi = 1;
      for (; l.hasNext();) {
        final char c = l.next();
        if (c == NEG_ONE) {
          assert value == 0 : "-1 should only occur at first char!";
          return Long.MIN_VALUE;
        }
        final long delta = (VALUE_TO_NUM[c]*multi);
        assert delta >= 0 : "Unexpected Long overlow" ;
        if (c < END_VALUE_BOUNDARY) {        // We hit the end of this number
          if (c > NEGATIVE_VALUE_BOUNDARY) {
            // And the number was not negative; just return the sum
            return value + delta;
          }
          // Note that we negate the value and the delta, as this will prevent
          // an integer overflow of Integer.MIN_VALUE.
          return -value - delta;
        }
        // For continuation digits, just accumulate the sum of each base 32 digit.
        value += delta;
        multi <<= 5; // multiply by 32
      }
      assert false : "Malformed encoded number: `" + l + "` (outer ticks added)";
      return value;
    }

  /**
   * Serializes an int according to the serialization policy defined in {@link #CONTINUATION_NUM_SECTION}.
   */
  @Override
  public String serializeInt(final int i) {
    CharacterBuffer buffer = computeSerialization(i);
    return buffer.serialize();
  }

  /**
   * Serializes a long according to the serialization policy defined in {@link #CONTINUATION_NUM_SECTION}.
   */
  @Override
  public String serializeLong(final long l) {
    CharacterBuffer buffer = computeSerialization(l);
    return buffer.serialize();
  }

  /**
   * Computes a linked list of serialization results for the supplied integer.
   * <p>
   * See {@link #CONTINUATION_NUM_SECTION} for a detailed description of the serialization policy.
   */
  protected CharacterBuffer computeSerialization(int i) {
    boolean negative;
    final CharacterBuffer head = new CharacterBuffer();
    CharacterBuffer tail = head;
    if (i < 0) {
      negative = true;
      if (i == Integer.MIN_VALUE) {
        // MIN_VALUE actually falls outside of our dual-32 bit address space;
        // If you try to count up to MAX_VALUE and then invert the result, it will overflow
        head.c = NEG_ONE;
        head.slot ++;
        return head;
      } else {
        i = -i;
      }
      assert i >= 0;
    } else {
      negative = false;
    }
    for ( int pos = head.slot + 1; ; pos++ ) {
      final int chunk = i%32;
      i = i/32;
      if (i == 0) {
        return terminate(negative, head, tail, chunk);
      }
      tail = pushItem(pos, chunk, head, tail);
    }
  }

  private CharacterBuffer pushItem(final int slot, final int value, final CharacterBuffer head, CharacterBuffer tail) {
    tail.c = value == -1 ? NEG_ONE : CONTINUATION_NUM_SECTION[value];
    head.slot ++;
    final CharacterBuffer next = new CharacterBuffer();
    next.slot = slot;
    tail.next = next;
    return next;
  }

  /**
   * Computes a linked list of serialization results for the supplied long.
   * <p>
   * See {@link #CONTINUATION_NUM_SECTION} for a detailed description of the serialization policy.
   */
  private CharacterBuffer computeSerialization(long i) {
    boolean negative;
    final CharacterBuffer head = new CharacterBuffer();
    CharacterBuffer tail = head;
    if (i < 0) {
      negative = true;
      if (i == Long.MIN_VALUE) {
          // MIN_VALUE actually falls outside of our dual-32 bit address space;
          // If you try to count up to MAX_VALUE and then invert the result, it will overflow
        head.c = NEG_ONE;
        head.slot++;
        return head;
      } else {
        i = -i;
      }
      assert i >= 0;
    } else {
      negative = false;
    }
    for ( int pos = head.slot + 1 ; ; pos++ ) {
      final int chunk = (int)(i%32L);
      i = i/32L;
      if (i == 0) {
        return terminate(negative, head, tail, chunk);
      }
      tail = pushItem(pos, chunk, head, tail);
    }
  }

    private CharacterBuffer terminate(boolean negative, CharacterBuffer head, CharacterBuffer tail, int chunk) {
        head.slot ++;
        if (negative) {
            tail.c = NEGATIVE_NUM_ENDING[chunk];
        } else {
            tail.c = POSITIVE_NUM_ENDING[chunk];
        }
        return head;
    }

    @Override
  public String serializeBoolean(final boolean z) {
    return z ? "1" : "0";
  }

  @Override
  public String serializeBooleanArray(final boolean ... z) {
    final int size = z.length / 5 + 1;
    // We will write a full large int using as many base 32 values as we need.
    CharacterBuffer sizeChunk = computeSerialization(z.length);
    final int offset = sizeChunk.slot;
    final char[] buffer = new char[size + offset];
    sizeChunk.slot = 0;
    while (sizeChunk != null) {
      buffer[sizeChunk.slot] = sizeChunk.c;
      sizeChunk = sizeChunk.next;
    }
    for (int i = 0; i < size; i++) {
      final int value = boolsToBase32(z, i);
      assert value < 32;
      buffer[i+offset] = POSITIVE_NUM_ENDING[value%32];
    }
    return String.valueOf(buffer);
  }

  private int boolsToBase32(final boolean[] z, int pos) {
    int value = 0;
    if (pos+5 >= z.length) {
      // This value is less than five booleans.  Use a loop
      final int start = pos;
      for (;pos < z.length; pos++ ) {
        if (z[pos]) {
          value += BIT_MASKS[pos-start];
        }
      }
      return value;
    } else {
      // We know we have at least five booleans we can read
      if (z[pos++]) {
        value += 1;
      }
      if (z[pos++]) {
        value += 2;
      }
      if (z[pos++]) {
        value += 4;
      }
      if (z[pos++]) {
        value += 8;
      }
      if (z[pos++]) {
        value += 16;
      }
      return value;
    }
  }

  @Override
  public String serializeByte(final byte b) {
    return serializeInt(b);
  }

  @Override
  public String serializeShort(final short s) {
    return serializeInt(s);
  }

  @Override
  public String serializeChar(final char c) {
    return serializeInt(c);
  }

  @Override
  public String serializeFloat(final float f) {
    return serializeInt(Float.floatToIntBits(f));
  }

  @Override
  public String serializeDouble(final double d) {
    return serializeLong(Double.doubleToLongBits(d));
  }

  @Override
  public boolean deserializeBoolean(final CharIterator z) {
    return z.next() == '1';
  }

  @Override
  public boolean[] deserializeBooleanArray(final CharIterator z) {
    final int size = deserializeInt(z);
    final boolean[] result = new boolean[size];
    for (int i = 0; ; ) {
      final int value = deserializeInt(z);
      if (i + 5 > size) {
        // The last value; may not have all five booleans; use a loop
        final int start = i;
        for (;i < size; i++) {
          result[i] = (value & BIT_MASKS[i-start]) != 0;
        }
        break;
      } else {
        // Write five more booleans
        result[i++] = (value & 1) != 0;
        result[i++] = (value & 2) != 0;
        result[i++] = (value & 4) != 0;
        result[i++] = (value & 8) != 0;
        result[i++] = (value & 16) != 0;
      }
    }
    return result;
  }

  @Override
  public byte deserializeByte(final CharIterator b) {
    return (byte)deserializeInt(b);
  }

  @Override
  public short deserializeShort(final CharIterator s) {
    return (short)deserializeInt(s);
  }

  @Override
  public char deserializeChar(final CharIterator c) {
    return (char)deserializeInt(c);
  }

  @Override
  public float deserializeFloat(final CharIterator f) {
    final int asInt = deserializeInt(f);
    return Float.intBitsToFloat(asInt);
/**
For javascript, we will use a native function to get our int bits:
function FloatToIEEE(f)
{
  var buf = new ArrayBuffer(4);
  (new Float32Array(buf))[0] = f;
  return (new Uint32Array(buf))[0];
}
*/
  }

  @Override
  public double deserializeDouble(final CharIterator d) {
    final long asLong = deserializeLong(d);
    return Double.longBitsToDouble(asLong);
/**
For javascript, we wil use a native function to get our long bits:
function DoubleToIEEE(f)
{
  var buf = new ArrayBuffer(8);
  (new Float64Array(buf))[0] = f;
  // We will also process these bits as ints to avoid long emulation.
  // Thus, we do not bother with a doubleToLongBits method, as long emulation sucks
  return [ (new Uint32Array(buf))[0] ,(new Uint32Array(buf))[1] ];
}
*/
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeString(xapi.source.lex.CharIterator)
   */
  @Override
  public String deserializeString(final CharIterator s) {
    final int size = deserializeInt(s);
    if (size == -1) {
      return null;
    }
    if (size == 0) {
      return "";
    }
    return s.consume(size).toString();
  }

  @Override
  public String serializeString(final String s) {
    if (s == null) {
      return serializeInt(-1);
    }
    return serializeInt(s.length()) + s;
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeClass(java.lang.Class)
   */
  @Override
  public String serializeClass(final Class<?> c) {
    return serializeString(c == null ? null : c.getName());
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeClass(xapi.source.lex.CharIterator)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Class<?> deserializeClass(final CharIterator c) {
    final String cls = deserializeString(c);
    return loadClass(cls);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<?> loadClass(final String cls) {
    if (cls == null) {
      return null;
    }
    try {
      switch (cls) {
        case "boolean":
          return boolean.class;
        case "byte":
          return byte.class;
        case "short":
          return short.class;
        case "char":
          return char.class;
        case "int":
          return int.class;
        case "long":
          return long.class;
        case "float":
          return float.class;
        case "double":
          return double.class;
        case "[Lboolean;":
          return boolean[].class;
        case "[Lbyte;":
          return byte[].class;
        case "[Lshort;":
          return short[].class;
        case "[Lchar;":
          return char[].class;
        case "[Lint;":
          return int[].class;
        case "[Llong;":
          return long[].class;
        case "[Lfloat;":
          return float[].class;
        case "[Ldouble;":
          return double[].class;
        case "void":
          return void.class;
        case "xapi.collect.api.IntTo":
          return IntTo.class;
        case "xapi.collect.api.StringTo":
          return StringTo.class;
        case "xapi.collect.api.ClassTo":
          return ClassTo.class;
        case "xapi.collect.api.ObjectTo":
          return ObjectTo.class;
        case "xapi.collect.api.IntTo.Many":
          return IntTo.Many.class;
        case "xapi.collect.api.StringTo.Many":
          return StringTo.Many.class;
        case "xapi.collect.api.ClassTo.Many":
          return ClassTo.Many.class;
        case "xapi.collect.api.ObjectTo.Many":
          return ObjectTo.Many.class;
        case "java.time.Duration":
          return Duration.class;
        // Cannot support standard collections due to inability to determine properly erased component types
//        case "java.util.List":
//          return List.class;
//        case "java.util.ArrayList":
//          return ArrayList.class;
//        case "java.util.LinkedList":
//          return LinkedList.class;
//        case "java.util.Set":
//          return Set.class;
//        case "java.util.HashSet":
//          return HashSet.class;
//        case "java.util.LinkedHashSet":
//          return LinkedHashSet.class;
//        case "java.util.TreeSet":
//          return TreeSet.class;
//        case "java.util.Map":
//          return Map.class;
//        case "java.util.HashMap":
//          return HashMap.class;
//        case "java.util.LinkedHashMap":
//          return LinkedHashMap.class;
//        case "java.util.TreeMap":
//          return TreeMap.class;
      }
      return Class.forName(cls);
    } catch (final ClassNotFoundException e) {
      try {
        return Thread.currentThread().getContextClassLoader().loadClass(cls);
      } catch (ClassNotFoundException e1) {
        assert false : "Could not deserialize class "+cls+"; make sure that reflection " +
            "is enabled for this type.\n" +
            ( X_Runtime.isGwt()
            ? "Calling Class.forName(\""+cls+"\"); with a string literal should suffice;\n"
            : "Check your classpath:\n" + System.getProperty("java.class.path", "").replace(File.pathSeparatorChar, '\n')
        );
      }
      throw X_Debug.rethrow(e);
    }
  }

}
