package xapi.test.gwt.inject;

import xapi.annotation.inject.SingletonDefault;
import xapi.log.X_Log;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;

@SingletonDefault(implFor=ImportTestReceiver.class)
public class ImportTestCallback_NeverCalled implements ImportTestReceiver{

  public static final String DO_NOT_INCLUDE = "Never Included";
  
  @Override
  public void set(ImportTestInterface value) {
    X_Log.info(DO_NOT_INCLUDE);
  }

}
