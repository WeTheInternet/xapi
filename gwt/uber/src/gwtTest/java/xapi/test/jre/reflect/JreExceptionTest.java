package xapi.test.jre.reflect;

import org.junit.Test;

public class JreExceptionTest {

  
  @Test(expected = NoSuchMethodException.class)
  public void testMissingMethod() throws Exception {
    JreExceptionTest.class.getMethod("DoesntExist", String.class);
  }
  
}
