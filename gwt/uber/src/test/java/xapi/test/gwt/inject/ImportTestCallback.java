package xapi.test.gwt.inject;

import org.junit.Assert;

import xapi.annotation.inject.SingletonDefault;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;
import xapi.test.gwt.inject.cases.ImportTestImplementation;
import xapi.util.api.ReceivesValue;

@SingletonDefault(implFor=ImportTestCallback.class)
public class ImportTestCallback implements ReceivesValue<ImportTestInterface>{

  @Override
  public void set(ImportTestInterface importedService) {
    Assert.assertNotSame("Deferred binding failed for ImportTestInterface", ImportTestImplementation.class, importedService.getClass());
    xapi.log.X_Log.info("Imported! "+importedService);
    importedService.service();
  }
}
