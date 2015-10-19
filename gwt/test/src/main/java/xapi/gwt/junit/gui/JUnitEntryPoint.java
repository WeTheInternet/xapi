package xapi.gwt.junit.gui;

import com.google.gwt.core.client.EntryPoint;

/**
 * Created by james on 16/10/15.
 */
public abstract class JUnitEntryPoint extends JUnitGui implements EntryPoint {

  @Override
  public void onModuleLoad() {
    runAllTests();
  }


}
