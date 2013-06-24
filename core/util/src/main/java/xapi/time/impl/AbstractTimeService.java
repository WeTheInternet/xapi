package xapi.time.impl;

import xapi.time.api.Moment;
import xapi.time.service.TimeService;

public abstract class AbstractTimeService extends ImmutableMoment implements TimeService {

  private static final long serialVersionUID = 1130197439830993337L;

  public AbstractTimeService() {
    super(System.currentTimeMillis());
  }

  protected double now;
  private double delta;

  @Override
  public double birth() {
    return super.millis();
  }

  @Override
  public Moment now() {
    return new ImmutableMoment(System.currentTimeMillis());
  }

  @Override
  public Moment clone(Moment moment) {
    return new ImmutableMoment(moment.millis());
  }

  @Override
  public double millis() {
    return now;
  }

  @Override
  public Moment nowPlusOne() {
    double later;
    // locks on static var
    synchronized (TimeService.second_to_nano) {
      later = now+(delta += TimeService.second_to_nano);// ensured unique
    }
    return new ImmutableMoment(later);
  }

  @Override
  public void tick() {
    // low precision, but deterministic and atomic
    now = System.currentTimeMillis();
    delta = 0;
  }
  
}
