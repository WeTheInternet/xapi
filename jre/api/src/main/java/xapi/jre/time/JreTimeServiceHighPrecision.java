package xapi.jre.time;

import xapi.annotation.inject.SingletonOverride;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.time.impl.TimeServiceDefault;
import xapi.time.service.TimeService;

@JrePlatform
@AndroidPlatform
@SingletonOverride(implFor=TimeService.class)
public class JreTimeServiceHighPrecision extends TimeServiceDefault{

  private static final long serialVersionUID = 7851025085789327954L;

  private final double nanoBirth;
  
  private double nanoNow;

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
    synchronized(second_to_nano) {// Forces race conditions to wait;
      // if .nanoTime() in an enviro does not have nano-second resolution,
      // it probably doesn't have hardware fast enough to resolve the 
      // race condition faster than nanoTime() updates.
      now = System.nanoTime();
    }
    return new ImmutableMoment(birth()+
      (now-nanoBirth)*milli_to_nano);
  }


  
  @Override
  public Moment now() {
    return new ImmutableMoment(birth()+
      (System.nanoTime()-nanoBirth)*milli_to_nano);
  }

  @Override
  public synchronized void tick() {
    nanoNow = (milli_to_nano*(System.nanoTime()-nanoBirth));
  }
}
