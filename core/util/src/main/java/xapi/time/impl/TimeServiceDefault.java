package xapi.time.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.time.service.TimeService;

import java.util.Date;

@SingletonDefault(implFor = TimeService.class)
public class TimeServiceDefault extends AbstractTimeService {

  private static final long serialVersionUID = -8070323612852368910L;

  @Override
  public void runLater(Runnable runnable) {
    new Thread(runnable).start();
  }

  @Override
  public String timestamp() {
    return new Date().toString();
  }
}
