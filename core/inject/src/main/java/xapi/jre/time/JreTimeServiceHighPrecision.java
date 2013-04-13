package xapi.jre.time;

import xapi.annotation.inject.SingletonOverride;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;
import xapi.time.api.Moment;
import xapi.time.api.TimeService;
import xapi.time.impl.ImmutableMoment;
import xapi.time.impl.TimeServiceDefault;

@JrePlatform
@AndroidPlatform
@SingletonOverride(implFor=TimeService.class)
public class JreTimeServiceHighPrecision extends TimeServiceDefault{

  private static final long serialVersionUID = 7851025085789327954L;

  private final double nanoBirth;
  //don't let threads cache this value!
  private volatile double nanoNow;

  public JreTimeServiceHighPrecision() {
    nanoBirth = System.nanoTime();
  }

  @Override
  public double millis() {
    return (birth()+nanoNow);
  }

  @Override
  public Moment nowPlusOne() {
    float now;
    synchronized(nano) {
      now = System.nanoTime();
    }
    return new ImmutableMoment(birth()+
      (now-nanoBirth)*nano_to_milli);
  }

  protected static final Double nano_to_milli = 0.000001;

  
  @Override
  public Moment now() {
    return new ImmutableMoment(birth()+
      (System.nanoTime()-nanoBirth)*nano_to_milli);
  }

  @Override
  public void tick() {
    nanoNow = (nano_to_milli*(System.nanoTime()-nanoBirth));
  }
}
