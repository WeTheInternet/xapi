package xapi.server.gwtc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RemoteClasspathServiceTest {

  @Test
  public void testGwtModuleRenameDetection() {
    RemoteClasspathServiceImpl impl = new RemoteClasspathServiceImpl();
    String longName = impl.getLongName("GwtcTest");
    assertEquals("xapi.GwtcTest", longName);
  }
  
  
}
