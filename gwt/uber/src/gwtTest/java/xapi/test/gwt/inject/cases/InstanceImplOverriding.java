package xapi.test.gwt.inject.cases;

import com.google.gwt.core.shared.GWT;

import xapi.annotation.inject.InstanceOverride;
import xapi.log.X_Log;
import xapi.test.gwt.inject.InstanceInterface;

@InstanceOverride(implFor=InstanceInterface.class,priority=Integer.MAX_VALUE)
public class InstanceImplOverriding implements InstanceInterface{

	static{
		if (GWT.isClient())
			throw new RuntimeException("Deferred binding failed! This class should never be accessed in GWT");
	}
	
  @Override
  public void test() {
	  if (GWT.isClient()){
		  throw new RuntimeException("Deferred binding failed! This class should not be called in GWT");
	  }else{
		  X_Log.info("Instance injection success!");
	  }
  }

}
