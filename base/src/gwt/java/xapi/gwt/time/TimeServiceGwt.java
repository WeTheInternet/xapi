package xapi.gwt.time;

import com.google.gwt.i18n.client.TimeZone;
import xapi.annotation.inject.SingletonOverride;
import xapi.fu.Lazy;
import xapi.platform.GwtPlatform;
import xapi.time.api.TimeZoneInfo;
import xapi.time.impl.AbstractTimeService;
import xapi.time.service.TimeService;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;

import java.util.Date;

@GwtPlatform
@SingletonOverride(implFor=TimeService.class, priority=Integer.MIN_VALUE+4)
public class TimeServiceGwt extends AbstractTimeService {

  private static final long serialVersionUID = -7873490109878690176L;

  // TODO move this dependency out of X_Core so we don't have to depend on GWT I18N by default
  private final Lazy<DateTimeFormat> formatter = Lazy.deferred1(
      ()->DateTimeFormat.getFormat(PredefinedFormat.ISO_8601));

    private final Lazy<DateTimeFormat> humanFormatter = Lazy.deferred1(
            () -> DateTimeFormat.getFormat("MMM d, yyyy h:mm:ss a"));

    @Override
  public String timestamp() {
    return timestamp(Duration.currentTimeMillis());
  }

  @Override
  public String timestamp(double millis) {
    return formatter.out1().format(new Date((long)millis));
  }

    @Override
    public String timestampHuman() {
        return timestampHuman(Duration.currentTimeMillis());
    }

    @Override
    public String timestampHuman(double millis) {
        return humanFormatter.out1().format(new Date((long) millis));
    }

    @Override
  public void runLater(final Runnable runnable) {
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        runnable.run();
      }
    });
  }

    @Override
    public TimeZoneInfo systemZone() {
        final Date now = new Date();
        final TimeZone zone = TimeZone.createTimeZone(now.getTimezoneOffset());
        final String timeZoneId = zone.getISOTimeZoneString(now);
        final String zoneName = zone.getShortName(now);
        final boolean usesDst = zone.getStandardOffset() != zone.getOffset(now);
        return new TimeZoneInfo(timeZoneId, zoneName, zone.getStandardOffset(), usesDst);
    }
}
