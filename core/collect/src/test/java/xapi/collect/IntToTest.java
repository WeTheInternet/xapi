package xapi.collect;

import org.junit.Assert;
import org.junit.Test;

import xapi.collect.api.IntTo;
import xapi.collect.impl.IntToList;

public class IntToTest {

  @Test
  public void testIntoTo_ToArray() {
    IntTo<String> array = new IntToList<String>(String.class);
    array.add("1");
    String[] asArray = array.toArray();
    Assert.assertEquals(asArray[0], array.get(0));
  }
  
}
