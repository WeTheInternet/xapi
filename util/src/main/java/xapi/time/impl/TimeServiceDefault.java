package xapi.time.impl;

import xapi.annotation.inject.SingletonDefault;
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
}
