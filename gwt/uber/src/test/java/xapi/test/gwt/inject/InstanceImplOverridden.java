package xapi.test.gwt.inject;

import com.google.gwt.core.shared.GWT;

import xapi.annotation.inject.InstanceOverride;

@InstanceOverride(implFor=InstanceInterface.class,priority=Integer.MIN_VALUE)
public class InstanceImplOverridden implements InstanceInterface{

	//This type should always be overridden. Lets make sure it always bombs.
	static{
		if (GWT.isClient())
			throw new RuntimeException("InstanceImplOverridden should never be accessed!");
		if (!GWT.isClient())
			throw new RuntimeException("InstanceImplOverridden should never be accessed!");
	}
	
  @Override
  public void test() {
    throw new RuntimeException("This type should never be visible!");
  }
}
