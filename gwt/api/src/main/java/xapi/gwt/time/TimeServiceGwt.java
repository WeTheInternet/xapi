package xapi.gwt.time;

import java.util.Date;

import xapi.annotation.inject.SingletonOverride;
import xapi.inject.impl.SingletonProvider;
import xapi.platform.GwtPlatform;
import xapi.time.impl.AbstractTimeService;
import xapi.time.service.TimeService;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;

@GwtPlatform
@SingletonOverride(implFor=TimeService.class, priority=Integer.MIN_VALUE+1)
public class TimeServiceGwt extends AbstractTimeService {

  private static final long serialVersionUID = -7873490109878690176L;
  
  private final SingletonProvider<DateTimeFormat> formatter = new SingletonProvider<DateTimeFormat>() {
    protected DateTimeFormat initialValue() {
      return DateTimeFormat.getFormat(PredefinedFormat.ISO_8601);
    };
  };
  
  @Override
  public String timestamp() {
    return formatter.get().format(new Date());
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
