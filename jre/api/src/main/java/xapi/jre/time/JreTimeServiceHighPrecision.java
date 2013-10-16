package xapi.jre.time;

import java.util.Calendar;

import xapi.annotation.inject.SingletonOverride;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.time.impl.TimeServiceDefault;
import xapi.time.service.TimeService;

@JrePlatform
@AndroidPlatform
@SingletonOverride(implFor=TimeService.class,priority=Integer.MIN_VALUE+2)
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
  
  @Override
  public String timestamp() {
    // Ya...  It's more lines of code than using a library,
    // but it's also the minimum overhead possible.
    Calendar c = Calendar.getInstance();
    char[] result = "yyyy-MM-ddTHH:mm.sss+00:00".toCharArray();
    // yyyy
    int val = c.get(Calendar.YEAR);
    result[3] = Character.forDigit(val%10, 10);
    result[2] = Character.forDigit((val/=10)%10, 10);
    result[1] = Character.forDigit((val/=10)%10, 10);
    result[0] = Character.forDigit((val/10)%10, 10);
    
    val = c.get(Calendar.MONTH);
    result[5] = Character.forDigit(val/10, 10);
    result[6] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.DATE);
    result[8] = Character.forDigit(val/10, 10);
    result[9] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.HOUR);
    result[11] = Character.forDigit(val/10, 10);
    result[12] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.MINUTE);
    result[14] = Character.forDigit(val/10, 10);
    result[15] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.MILLISECOND);
    result[19] = Character.forDigit(val%10, 10);
    result[18] = Character.forDigit((val/=10)%10, 10);
    result[17] = Character.forDigit((val/10)%10, 10);
    
    val = c.getTimeZone().getOffset(c.getTimeInMillis()) / 60000;
    if (val < 0) {
      result[20] = '-';
      val = -val;
    }
    int hours = val / 60;
    result[21] = Character.forDigit(hours/10, 10);
    result[22] = Character.forDigit(hours%10, 10);
    val = val%60;
    result[24] = Character.forDigit((val/10)%10, 10);
    result[25] = Character.forDigit(val%10, 10);
    
    return new String(result);
  }
  
}
