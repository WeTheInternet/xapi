package xapi.dev.components;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class WebComponentBinderGenerator extends IncrementalGenerator {

	@Override
  public RebindResult generateIncrementally(TreeLogger logger,
      GeneratorContext context, String typeName)
      throws UnableToCompleteException {
		
	  return new RebindResult(RebindMode.USE_ALL_CACHED, typeName);
  }

	@Override
  public long getVersionId() {
	  return 0;
  }

}
