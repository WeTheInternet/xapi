package com.google.gwt.reflect.test;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.user.client.Window;

public class ObfuscatedJUnitTest extends GWTTestCase{

  
  static final Class<Scheduler> scheduler = GwtReflect.magicClass(Scheduler.class);
  static final Class<Duration> duration = GwtReflect.magicClass(Duration.class);
  
  public void testScheduler() {
    assertTrue(scheduler instanceof Class);
    Window.alert(scheduler.getClass()+"");
    Window.alert(scheduler+"");
    Window.alert(scheduler+" "+scheduler.getName());
  }

  public void testDuration() {
    assertTrue(duration instanceof Class);
    Window.alert(duration.getClass()+"");
    Window.alert(duration+"");
    Window.alert(duration+" "+duration.getName());
  }
  

  @Override
  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";  
  }
}
