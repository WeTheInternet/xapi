package xapi.source;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class X_SourceTest {

  @Test
  public void testSplitClassName_SimpleValid() {
    String[] result = X_Source.splitClassName("test.Class");
    Assert.assertArrayEquals(new String[]{"test","Class"}, result);
  }
  
  @Test
  public void testSplitClassName_SimpleValidSubclass() {
    String[] result = X_Source.splitClassName("test.Class.Subclass");
    Assert.assertArrayEquals(new String[]{"test","Class.Subclass"}, result);
  }
  
  @Test
  public void testSplitClassName_NoPackageSimpleName() {
    String[] result = X_Source.splitClassName("Class");
    Assert.assertArrayEquals(new String[]{"","Class"}, result);
  }
  
  @Test
  public void testSplitClassName_NoPackageComplexName() {
    String[] result = X_Source.splitClassName("Class.Subclass");
    Assert.assertArrayEquals(new String[]{"","Class.Subclass"}, result);
  }
  
}
