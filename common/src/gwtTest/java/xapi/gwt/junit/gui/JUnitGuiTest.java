package xapi.gwt.junit.gui;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.reflect.shared.GwtReflect;

/**
 * Created by james on 16/10/15.
 */
public class JUnitGuiTest extends JUnitGui implements EntryPoint {
  @Override
  public void onModuleLoad() {
    runAllTests();
  }

  @Override
  protected Class[] testClasses() {
    return new Class[]{
        GwtReflect.magicClass(DummyTest.class)
    };
  }
}
