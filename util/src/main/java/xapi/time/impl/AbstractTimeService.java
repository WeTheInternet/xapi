package xapi.time.impl;

import xapi.time.api.Moment;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;
import xapi.time.service.TimeService;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractTimeService extends ImmutableMoment implements TimeService {

  private static final long serialVersionUID = 1130197439830993337L;

  public AbstractTimeService() {
    super(System.currentTimeMillis());
    delta = new AtomicReference<>(birth());
    marginOfError = 1;
  }

  protected volatile double now;
  protected final AtomicReference<Double> delta;
  private double marginOfError;

  @Override
  public double birth() {
    return super.millis();
  }

  @Override
  public Moment now() {
    return new ImmutableMoment(Math.max(delta.get(), millis()));
  }

  @Override
  public Moment clone(Moment moment) {
    return new ImmutableMoment(moment.millis());
  }

  @Override
  public double millis() {
    return System.currentTimeMillis();
  }

  @Override
  public double lastTick() {
    return delta.get();
  }

  @Override
  public Moment nowPlusOne() {
    return new ImmutableMoment(tick());
  }

    @Override
    public TimeComponents breakdown(final double epochMillis, final TimeZoneInfo zone) {
        return new TimeComponents(epochMillis, zone);
    }

    @Override
    public TimeZoneInfo systemZone() {
        throw new UnsupportedOperationException("systemZone not supported in " + getClass().getSimpleName());
    }

    @Override
    public String formatTime(final int hour24, final int minute) {
        final boolean pm = hour24 >= 12;
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        final String ampm = pm ? "pm" : "am";
        return hour12 + (minute < 10 ? "0" : "") + minute + " " + ampm;
    }

    @Override
    public double toStartOfWeek(final double epochMillis, final TimeZoneInfo zone) {
        throw new UnsupportedOperationException("startOfWeek not supported in " + getClass().getSimpleName());
    }

    /**
   * The number of slices between a millisecond,
   * valid until Wed Sep 07 2039 08:47:35 GMT-7,
   * which is the unix timestamp 0x200_0000_0000
   * (when the significand of timestamps rolls over to the 42nd bit).
   *
   * This is the smallest number you can safely increment
   * a double precision floating point representation of a timestamp
   * and not "lose" the increment by having it rounded off by floating point math.
   *
   * This will need to be bumped up in 2039 to ensure all math stays precise.
   *
   * From Math.ulp:
   *
   * An ulp, unit in the last place, of a double value is the positive
   * distance between this floating-point value and the double value next larger in magnitude
   *
   */
  protected static final double TIME_ULP = 1./0x1000;

  @Override
  public double tick() {
    // As nasty as this is, it is the only way to ensure that
    // we produce monatomically increasing double values.
    double newNow = System.currentTimeMillis();
    // use atomic reference to get long-lived (if necessary) CAS semantics
    return delta.updateAndGet(i->{
      if (i < newNow) {
        return newNow;
      }
      double next = i + TIME_ULP;
      // on very fast systems that hit this method very quickly,
      // we will request more double values than can fit between
      // milliseconds, to ensure that we never return a value that is greater than the
      // current system time in milliseconds.
      if (next >= getMarginOfError() + newNow) { // check if we would return a value in the future...
        while (newNow == System.currentTimeMillis()) {
          // We'll park to give other threads a chance to pick up work.
          // When testing with daemon threads, this greatly reduces the
          // number of times we spin here.  This was tested by counting
          // how many times this loop runs more than once, when there are
          // a fairly large number of daemons running;
          LockSupport.parkNanos(1);
        }
        next = System.currentTimeMillis();
      }
      return next;
    });
  }

  /**
   * @return The fractional number of milliseconds ahead of the system clock we may generate nowPlusOne() values.
   *
   * Default is 1 millisecond.  Smaller values will allow for fewer spaces between milliseconds,
   * while larger values will prevent increments from blocking if nowPlusOne is call more than 0x1000 in a millisecond.
   * (0x1000 is the most bits that can fit between java double precision floating point timestamps).
   *
   * Setting the margin of error to a large value will make the code run very fast,
   * at the risk of using timestamps in the semi-"distant" future;
   * when a nowPlusOne() or tick() call occurs many times in a millisecond,
   * this margin of error will cause the atomic increment to block until the system time is within the margin of error.
   *
   * Using a margin of error of 0 will cause tick() and nowPlusOne() to block until the system time catches up,
   * and will perform very poorly when used concurrently.
   */
  public double getMarginOfError() {
    return marginOfError;
  }

  /**
   * @param marginOfError - A decimal amount of milliseconds that we will allow .tick() or nowPlusOne() to get ahead
   * of the current time in millis().
   *
   * Default is 1 millisecond.  Smaller values will allow for fewer spaces between milliseconds (less unique doubles),
   * while larger values will prevent increments from blocking if nowPlusOne is call more than 0x1000 in a millisecond.
   * (0x1000 is the most bits that can fit between java double precision floating point timestamps).
   *
   * Setting the margin of error to a large value will make the code run very fast,
   * at the risk of using timestamps in the semi-"distant" future;
   * when a nowPlusOne() or tick() call occurs many times in a millisecond,
   * this margin of error will cause the atomic increment to block until the system time is within the margin of error.
   *
   * Using a margin of error of 0 will cause tick() and nowPlusOne() to block until the system time catches up,
   * and will perform very poorly when used concurrently.
   *
   */

  public void setMarginOfError(double marginOfError) {
    this.marginOfError = Math.max(0, marginOfError);
  }

}
