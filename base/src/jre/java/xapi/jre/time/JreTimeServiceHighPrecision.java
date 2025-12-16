package xapi.jre.time;

import xapi.annotation.inject.SingletonOverride;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;
import xapi.time.impl.TimeServiceDefault;
import xapi.time.service.TimeService;
import xapi.string.X_String;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.LockSupport;

@JrePlatform
@AndroidPlatform
@SingletonOverride(implFor=TimeService.class,priority=Integer.MIN_VALUE+3)
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
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DATE),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.MILLISECOND),
        c.getTimeZone().getOffset(c.getTimeInMillis()) / 60000);
  }

    public String timestampHuman() {
        return timestampHuman(millis());
    }

    public String timestampHuman(double millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) millis);
        return X_String.toHumanTimestamp(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DATE),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE));
    }

    @Override
    public TimeZoneInfo systemZone() {
        final TimeZone tz = TimeZone.getDefault();
        final String id = tz.getID();
        final boolean inDstNow = tz.inDaylightTime(new Date());
        final String display = tz.getDisplayName(inDstNow, TimeZone.LONG);
        final int rawOffset = tz.getRawOffset();
        final boolean observesDst = tz.observesDaylightTime();
        return new TimeZoneInfo(id, display, rawOffset, observesDst);
    }

    @Override
    public String formatTime(final int hour24, final int minute) {
        final boolean pm = hour24 >= 12;
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        final String ampm = pm ? "pm" : "am";
        return String.format("%d:%02d %s", hour12, minute, ampm);
    }

    @Override
    public double toStartOfWeek(final double epochMillis, final TimeZoneInfo zone) {
        final Instant instant = Instant.ofEpochMilli((long) epochMillis);
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone.getId());
        } catch (Exception ignored) {
            zoneId = ZoneOffset.ofTotalSeconds(zone.getOffsetAt(epochMillis) / 1000);
        }
        ZonedDateTime zdt = instant.atZone(zoneId);
        // DayOfWeek: Mon=1..Sun=7; want Sunday start, so subtract value%7 days
        int daysToSubtract = zdt.getDayOfWeek().getValue() % 7;
        ZonedDateTime startOfWeek = zdt.minusDays(daysToSubtract)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        return startOfWeek.toInstant().toEpochMilli();
    }


}
