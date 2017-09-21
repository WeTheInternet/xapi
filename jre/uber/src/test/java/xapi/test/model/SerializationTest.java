/**
 *
 */
package xapi.test.model;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.source.impl.StringCharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class SerializationTest {

  int iterations;
  private PrimitiveSerializerDefault serializer;

  @Before
  public void before() {
    iterations = 0xffff;
    serializer = new PrimitiveSerializerDefault();
  }

  @Test
  public void testIntegerSerialization() {

    String serialized = serializer.serializeInt(Integer.MAX_VALUE);
    int deserialized = serializer.deserializeInt(new StringCharIterator(serialized));
    assertEquals(Integer.MAX_VALUE, deserialized);

    serialized = serializer.serializeInt(Integer.MAX_VALUE-1);
    deserialized = serializer.deserializeInt(new StringCharIterator(serialized));
    assertEquals(Integer.MAX_VALUE-1, deserialized);

    serialized = serializer.serializeInt(Integer.MIN_VALUE+1);
    deserialized = serializer.deserializeInt(new StringCharIterator(serialized));
    assertEquals(Integer.MIN_VALUE+1, deserialized);

    serialized = serializer.serializeInt(Integer.MIN_VALUE);
    deserialized = serializer.deserializeInt(new StringCharIterator(serialized));
    assertEquals(Integer.MIN_VALUE, deserialized);

    for (int i = iterations; i --> -iterations;) {
      serialized = serializer.serializeInt(i);
      deserialized = serializer.deserializeInt(new StringCharIterator(serialized));
      assertEquals(i, deserialized);
    }
  }

  @Test
  public void testLongSerialization() {

    String serialized = serializer.serializeLong(Long.MAX_VALUE);
    long deserialized = serializer.deserializeLong(new StringCharIterator(serialized));
    Assert.assertEquals(Long.MAX_VALUE, deserialized);

    serialized = serializer.serializeLong(Long.MAX_VALUE-1);
    deserialized = serializer.deserializeLong(new StringCharIterator(serialized));
    Assert.assertEquals(Long.MAX_VALUE-1, deserialized);

    serialized = serializer.serializeLong(Long.MIN_VALUE);
    deserialized = serializer.deserializeLong(new StringCharIterator(serialized));
    Assert.assertEquals(Long.MIN_VALUE, deserialized);

    serialized = serializer.serializeLong(Long.MIN_VALUE+1);
    deserialized = serializer.deserializeLong(new StringCharIterator(serialized));
    Assert.assertEquals(Long.MIN_VALUE+1, deserialized);

    for (long i = iterations; i --> -iterations;) {
      final long value = i*(i-1)*(i+7);
      serialized = serializer.serializeLong(value);
      deserialized = serializer.deserializeLong(new StringCharIterator(serialized));
      Assert.assertEquals(value, deserialized);
    }
  }

  @Test
  public void testDoubleSerialization() {


    String serialized = serializer.serializeDouble(Double.MAX_VALUE);
    double deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.MAX_VALUE, deserialized, 0);

    serialized = serializer.serializeDouble(Double.MAX_VALUE-1);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.MAX_VALUE-1, deserialized, 0);

    serialized = serializer.serializeDouble(Double.MIN_VALUE);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.MIN_VALUE, deserialized, 0);

    serialized = serializer.serializeDouble(Double.MIN_VALUE+1);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.MIN_VALUE+1, deserialized, 0);

    serialized = serializer.serializeDouble(Double.POSITIVE_INFINITY);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.POSITIVE_INFINITY, deserialized, 0);

    serialized = serializer.serializeDouble(Double.NEGATIVE_INFINITY);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.NEGATIVE_INFINITY, deserialized, 0);

    serialized = serializer.serializeDouble(Double.NaN);
    deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
    assertEquals(Double.NaN, deserialized, 0);


    for (double i = iterations; i --> -iterations;) {
      // Test large values
      double value = i*i/137.1 * i;
      serialized = serializer.serializeDouble(value);
      deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
      assertEquals(value, deserialized, 0);

      // Test small values.  Small values, and 1.0/0 (infinity)
      value = 9.1/value;
      serialized = serializer.serializeDouble(value);
      deserialized = serializer.deserializeDouble(new StringCharIterator(serialized));
      assertEquals(value, deserialized, 0);

    }
  }

  @Test
  public void testBooleanArraySerialization() {
    for (int i = 100; i --> 0;) {
      boolean[] testArray = testBooleans(i, 0);
      String serialized = serializer.serializeBooleanArray(testArray);
      boolean[] deserialized = serializer.deserializeBooleanArray(new StringCharIterator(serialized));
      Assert.assertArrayEquals("Array serialization failed for array of size "+i,testArray, deserialized);

      testArray = testBooleans(i, 1);
      serialized = serializer.serializeBooleanArray(testArray);
      deserialized = serializer.deserializeBooleanArray(new StringCharIterator(serialized));
      Assert.assertArrayEquals("Array serialization failed for array of size "+i,testArray, deserialized);
    }
  }

  @Test
  public void testClassSerialization() {
    // First, test primitive class serialization
    Class<?> cls = int.class;
    String serialized = serializer.serializeClass(cls);
    Class<?> deserialized = serializer.deserializeClass(new StringCharIterator(serialized));
    Assert.assertEquals(cls, deserialized);

    // Next, a simple test class
    cls = SerializationTest.class;
    serialized = serializer.serializeClass(cls);
    deserialized = serializer.deserializeClass(new StringCharIterator(serialized));
    Assert.assertEquals(cls, deserialized);

    // Now, an array type
    cls = SerializationTest[].class;
    serialized = serializer.serializeClass(cls);
    deserialized = serializer.deserializeClass(new StringCharIterator(serialized));
    Assert.assertEquals(cls, deserialized);

    cls = long[].class;
    serialized = serializer.serializeClass(cls);
    deserialized = serializer.deserializeClass(new StringCharIterator(serialized));
    Assert.assertEquals(cls, deserialized);
  }

  /**
   * @param i
   * @return
   */
  private boolean[] testBooleans(int i, final int order) {
    final boolean[] test = new boolean[i];
    for (;i-->0;) {
      test[i] = i%2==order;
    }
    return test;
  }
}
