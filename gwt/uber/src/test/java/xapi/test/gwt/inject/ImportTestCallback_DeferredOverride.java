package xapi.test.gwt.inject;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

import xapi.log.X_Log;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;


public class ImportTestCallback_DeferredOverride implements ImportTestReceiver{

  public static final String MUST_INCLUDE = "Always Included";
  
  @Override
  public void set(ImportTestInterface value) {
    X_Log.info(MUST_INCLUDE);
    //pull in some extra dependencies to make sure they are getting sucked into the correct split point
    RootPanel.get().add(new PopupPanel());
  }

}
