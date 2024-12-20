package com.google.gwt.reflect.test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.shared.GwtReflect;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@ReflectionStrategy(magicSupertypes=false, keepCodeSource=true)
public class ArrayTests extends AbstractReflectionTest {

  public ArrayTests() {}

  class Child {

  }
  class Parent {
    List<Child> list;
    public Child[] array;
  }

  @Test
  public void testComponentTypes() throws NoSuchFieldException, IllegalAccessException {
    final Field arrayField = Parent.class.getField("array");
    Parent parent = new Parent();
    Child[] children = { new Child() };
    arrayField.set(parent, children);
    assertEquals(children, parent.array);
    final Object get = arrayField.get(parent);
    assertEquals(children, get);
  }

  @Test
  public void testSingleDimPrimitive() {
    long[] longs = (long[])Array.newInstance(long.class, 5);
    assertEquals(longs.length, 5);
    assertEquals(longs.getClass(), long[].class);
    longs[0] = 1;
    Array.setLong(longs, 1, 2);
    Array.setLong(longs, 2, longs[0] + 2);
    Array.setLong(longs, 3, Array.getLong(longs, 2) +1);
    Array.setLong(longs, 4, Array.getLength(longs));
    assertTrue("Arrays not equals", Arrays.equals(longs, new long[] {1,2,3,4,5}));
  }


  @Test
  public void testSingleDimComplex() {
    long[][] longs = (long[][])Array.newInstance(long[].class, 5);
    assertEquals(5, longs.length);
    assertEquals(null, longs[0]);
    assertEquals(long[][].class, longs.getClass());

    long[] subArr = longs[0] = new long[3];
    subArr[0] = 1;
    Array.setLong(subArr, 1, 2);
    Array.setLong(subArr, 2, subArr[0] + 2);

    Array.set(longs, 1, new long[3]);
    subArr = (long[])Array.get(longs, 1);
    Array.setLong(subArr, 0, 1);
    subArr[1] = Array.getLong(subArr, 0) +1;
    Array.setLong(subArr, 2, Array.getLength(subArr));
    assertTrue("Arrays not equals", Arrays.deepEquals(longs, new long[][] {
      new long[]{1,2,3},new long[]{1,2,3}, null, null, null
    }));
  }

  @Test
  public void testArrayEqualsSanity() {
    long[][] arrays = new long[][]{
      new long[]{1,2}, new long[]{3,4}
    }
    , arrays2 = new long[][]{
        new long[]{0,2}, new long[]{3,4}
    };
    assertTrue("Arrays not equals", Arrays.deepEquals(arrays, arrays));
    assertFalse("Arrays.deepEquals fail", Arrays.deepEquals(arrays, arrays2));
  }

  @Test
  public void testSingleDimObject() {
    Long[] longs = (Long[])Array.newInstance(Long.class, 5);
    assertEquals(longs.length, 5);
    assertEquals(longs.getClass(), Long[].class);
    longs[0] = 1L;
    Array.set(longs, 1, 2L);
    Array.set(longs, 2, longs[0] + 2);
    Array.set(longs, 3, (Long)Array.get(longs, 2) +1);
    Array.set(longs, 4, new Long(Array.getLength(longs)));
    assertTrue("Arrays not equals", Arrays.equals(longs, new Long[] {1L,2L,3L,4L,5L}));
  }

  @Test
  public void testMultiDimPrimitive() {
    long[][] longs = (long[][])Array.newInstance(long.class, 2, 3);
    assertEquals(longs.length, 2);
    assertEquals(longs[0].length, 3);
    assertEquals(longs.getClass(), long[][].class);
    long[] subArr = longs[0];
    subArr[0] = 1;
    Array.setLong(subArr, 1, 2);
    Array.setLong(subArr, 2, subArr[0] + 2);
    subArr = (long[])Array.get(longs, 1);
    Array.setLong(subArr, 0, 1);
    subArr[1] = Array.getLong(subArr, 0) +1;
    Array.setLong(subArr, 2, Array.getLength(subArr));
    assertTrue("Arrays not equals", Arrays.deepEquals(longs, new long[][] {
      new long[]{1,2,3},new long[]{1,2,3}
    }));
  }

  @Test
  public void testMultiDimObject() {
    Long[][] longs = (Long[][])Array.newInstance(Long.class, 2, 3);
    assertEquals(longs.length, 2);
    assertEquals(longs[0].length, 3);
    assertEquals(longs.getClass(), Long[][].class);
    Long[] subArr = longs[0];
    subArr[0] = 1L;
    Array.set(subArr, 1, 2L);
    Array.set(subArr, 2, subArr[0] + 2);
    subArr = (Long[])Array.get(longs, 1);
    Array.set(subArr, 0, 1L);
    subArr[1] = (Long)Array.get(subArr, 0) +1;
    Array.set(subArr, 2, new Long(Array.getLength(subArr)));
    assertTrue("Arrays erroneously inequal", Arrays.deepEquals(longs, new Long[][] {
      new Long[]{1L,2L,3L},new Long[]{1L,2L,3L}
    }));
    assertFalse("Arrays erroneously equal", Arrays.deepEquals(longs, new Long[][] {
      new Long[]{1L,2L,3L},new Long[]{0L,2L,3L}
    }));
  }

  @Test
  public void testComplexDims() {
    long[][][][] longs = (long[][][][]) Array.newInstance(long[][].class, 2, 3);
    long[][][][] longsNone = (long[][][][]) Array.newInstance(long.class, 2, 1, 2, 3);
    long[][][][] longsOne = (long[][][][]) Array.newInstance(long[].class, 2, 3, 2);
    long[][][][] longsTwo = (long[][][][]) Array.newInstance(long[][].class, 2, 3);
    long[][][][] longsThree = (long[][][][]) Array.newInstance(long[][][].class, 2);

    assertTrue("longsNone not equals", Arrays.deepEquals(longsNone, new long[][][][]{
        {
            {
                {0, 0, 0},
                {0, 0, 0}
            }
        },
        {
            {
                {0, 0, 0},
                {0, 0, 0}
            }
        }
    }));

    assertTrue("longsOne not equals", Arrays.deepEquals(longsOne, new long[][][][]{
        {
            {null, null},
            {null, null},
            {null, null},
        },
        {
            {null, null},
            {null, null},
            {null, null},
        },
    }));

    assertTrue("longsTwo not equals", Arrays.deepEquals(longsTwo, new long[][][][]{
        {
            null,
            null,
            null,
        },
        {
            null,
            null,
            null,
        },
    }));

    assertTrue("longsThree not equals", Arrays.deepEquals(longsThree, new long[][][][]{
        null,
        null,
    }));

    // now do a little array manipulation to be sure those work

    long[][] one = new long[0][], two = new long[1][], three = new long[][]{ { 1}, {1} };
    Array.set(longs[0], 1, one);
    longs[1][0] = two;
    Array.set(longs[1], 1, three);
    assertTrue("Arrays not equals", Arrays.deepEquals(longs, new long[][][][] {
        {
            null,
            one,
            null
        },
        {
            two,
            three,
            null
        }
    }));

  }

}
