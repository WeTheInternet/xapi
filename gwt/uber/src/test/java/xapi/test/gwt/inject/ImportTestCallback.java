package xapi.test.gwt.inject;

import xapi.annotation.inject.SingletonDefault;
import xapi.fu.In1;
import xapi.test.Assert;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;
import xapi.test.gwt.inject.cases.ImportTestImplementation;
import xapi.util.api.ReceivesValue;

@SingletonDefault(implFor=ImportTestCallback.class)
public class ImportTestCallback implements In1<ImportTestInterface> {

  @Override
  public void in(ImportTestInterface importedService) {
    Assert.assertNotSame("Deferred binding failed for ImportTestInterface", ImportTestImplementation.class, importedService.getClass());
    xapi.log.X_Log.info("Imported! "+importedService);
    importedService.service();
  }
}
