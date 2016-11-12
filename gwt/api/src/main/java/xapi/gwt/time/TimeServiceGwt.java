package xapi.gwt.time;

import xapi.annotation.inject.SingletonOverride;
import xapi.fu.Lazy;
import xapi.inject.impl.SingletonProvider;
import xapi.platform.GwtPlatform;
import xapi.time.impl.AbstractTimeService;
import xapi.time.service.TimeService;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;

import java.util.Date;

@GwtPlatform
@SingletonOverride(implFor=TimeService.class, priority=Integer.MIN_VALUE+3)
public class TimeServiceGwt extends AbstractTimeService {

  private static final long serialVersionUID = -7873490109878690176L;

  // TODO move this dependency out of X_Core so we don't have to depend on GWT I18N by default
  private final Lazy<DateTimeFormat> formatter = Lazy.deferred1(
      ()->DateTimeFormat.getFormat(PredefinedFormat.ISO_8601));

  @Override
  public String timestamp() {
    return timestamp(Duration.currentTimeMillis());
  }

  @Override
  public String timestamp(double millis) {
    return formatter.out1().format(new Date((long)millis));
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
}
