package xapi.jre.time;

import xapi.annotation.inject.SingletonOverride;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;
import xapi.time.impl.TimeServiceDefault;
import xapi.time.service.TimeService;
import xapi.util.X_String;

import java.util.Calendar;
import java.util.concurrent.locks.LockSupport;

@JrePlatform
@AndroidPlatform
@SingletonOverride(implFor=TimeService.class,priority=Integer.MIN_VALUE+2)
public class JreTimeServiceHighPrecision extends TimeServiceDefault{

  private static final long serialVersionUID = 7851025085789327954L;

  private final double nanoBirth;

  public JreTimeServiceHighPrecision() {
    nanoBirth = System.nanoTime();
  }

  @Override
  public double millis() {
    return (birth()+
        (System.nanoTime() - nanoBirth) * nano_to_milli);
  }

  @Override
  public double tick() {
    final double start = millis();
    return delta.updateAndGet(i -> {
      // Unless tick is called very, very rapidly,
      // the next nanotime will be greater than our current time.
      if (start > i) {
        return start;
      }
      // When called quickly, we want to return the smallest possible
      // floating point that is greater than the previous time,
      // as well as the current returnable time...
      i = i + TIME_ULP;
      // However, we don't want to get more than a millisecond out of sync...
      // So, we will wait until our nowPlusOne is within 1 milli of now()
      while (i > millis() + getMarginOfError()) {
        // Release our thread so that instead of busy-waiting,
        // we will let other threads have a chance to do some work.
        LockSupport.parkNanos(1);
      }
      return i;
    });
  }

  @Override
  public String timestamp() {
    return timestamp(millis());
  }
  public String timestamp(double millis) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis((long)millis);
    return X_String.toTimestamp(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH),
        c.get(Calendar.DATE),
        c.get(Calendar.HOUR),
        c.get(Calendar.MINUTE),
        c.get(Calendar.MILLISECOND),
        c.getTimeZone().getOffset(c.getTimeInMillis()) / 60000);
  }

}
