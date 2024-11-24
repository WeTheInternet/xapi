package xapi.test.gwt.inject;

import xapi.annotation.inject.InstanceOverride;

import com.google.gwt.core.shared.GWT;

@InstanceOverride(implFor=InstanceInterface.class,priority=Integer.MIN_VALUE+1)
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
