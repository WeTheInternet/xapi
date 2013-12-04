package xapi.dev.gwtc.test.cases;

import xapi.annotation.compile.Resource;
import xapi.gwtc.api.Gwtc;
import xapi.log.X_Log;

import com.google.gwt.core.client.EntryPoint;

@Gwtc(
    includeGwtXml={
      @Resource("xapi.X_Inherit"),
    },
    includeSource=""
)
public class CaseEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    X_Log.info(getClass(), "Hello World");
  }
}
