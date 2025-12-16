package xapi.time.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;
import xapi.time.service.TimeService;

import java.util.Date;

@SingletonDefault(implFor = TimeService.class)
public class TimeServiceDefault extends AbstractTimeService {

  private static final long serialVersionUID = -8070323612852368910L;

  @Override
  public void runLater(Runnable runnable) {
    final Thread t = new Thread(runnable);
    t.setName("X_Time " + runnable);
//    t.setDaemon(true);
    t.start();
  }

  @Override
  public String timestamp() {
    return timestamp(millis());
  }

  @Override
  public String timestamp(double millis) {
    return new Date((long)millis).toString();
  }

    @Override
    public String timestampHuman() {
        return timestamp();
    }

    @Override
    public String timestampHuman(final double millis) {
        return timestamp(millis);
    }

    @Override
    public double toStartOfWeek(final double epochMillis, final TimeZoneInfo zone) {
        Date date = new Date((long) epochMillis);
        // Get day of week (0-6, where 0 is Sunday)
        @SuppressWarnings("deprecation")
        int dayOfWeek = date.getDay();
        // Calculate milliseconds to subtract to get to start of week (Sunday)
        long millisToSubtract = dayOfWeek * 24L * 60L * 60L * 1000L;
        // Subtract hours, minutes, seconds and millis of current day
        @SuppressWarnings("deprecation")
        long timeOfDay = (date.getHours() * 60L * 60L * 1000L) +
                (date.getMinutes() * 60L * 1000L) +
                (date.getSeconds() * 1000L) +
                (long) epochMillis % 1000L;
        return epochMillis - millisToSubtract - timeOfDay;
    }
}
