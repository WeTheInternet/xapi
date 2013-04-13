package xapi.test.gwt.collect;

import xapi.collect.api.StringTo;
import xapi.gwt.collect.StringToGwt;
import xapi.test.Assert;

import com.google.gwt.junit.client.GWTTestCase;

public class StringToGwtTest extends GWTTestCase{

  public void testKeySet() {
    StringTo<Object> map = StringToGwt.create(Object.class);
    map.put("zero", "zero");
    map.put("one", "one");
    map.put("two", "two");
    Assert.assertArrayEquals(map.keyArray(), new String[] {"zero", "one", "two"});
  }

  @Override
  public String getModuleName() {
    return "xapi.X_Uber";
  }

}
