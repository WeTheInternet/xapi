package xapi.time;

import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.Out1;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.time.service.TimeService;
import xapi.string.X_String;

import static xapi.inject.X_Inject.singleton;

public class X_Time {

  public static final int ONE_MINUTE = 60 * 1000;
  public static final int ONE_HOUR = ONE_MINUTE * 60;
  public static final int ONE_DAY = ONE_HOUR * 24;

  private X_Time() {}

  private static final TimeService
      service = singleton(TimeService.class);


  public static Moment now() {
    return service.now();
  }

  public static double nowMillis() {
    return now().millis();
  }

  public static double nowMillisLong() {
    return now().millisLong();
  }

  public static String printTimeSince(double millis) {
    return print(nowMillis() - millis);
  }

  /**
   * Returns the Moment the current thread began operation,
   * or the last call to {@link #tick()}.
   *
   * You are getting an immutable copy of a known snapshot in time.
   *
   * This is useful to perform an operation "once per run cycle",
   * provided you don't manually advance the timer in a sloppy fashion.
   *
   * @return -
   */
  public static Moment threadStart() {
    return new ImmutableMoment(service.millis());
  }

  /**
   * Advances {@link #threadStart()} to current time.
   *
   * This will be called automatically when any task scheduled through X_Time
   * begins its execution cycle; whenever a process wakes from sleep or gets
   * pulled off a work queue and ran, the current moment will advance.
   *
   *
   */
  public static void tick() {
    service.tick();
  }
  /**
   * Creates an immutable copy of a moment.
   * Useful in case you create a mutable su
   *
   * @param moment
   * @return
   */
  public static Moment clone(final Moment moment) {
    return service.clone(moment);
  }

  /**
   * @return Milliseconds since epoch
   * when the static timeservice was instantiated (X_Time first accessed).
   */
  public static double birth() {
    return service.birth();
  }

  public static void doLater(final Do runnable) {
    service.runLater(runnable.toRunnable());
  }

  public static void doLaterUnsafe(final DoUnsafe runnable) {
    service.runLater(runnable.toRunnable());
  }

  public static void runLater(final Runnable runnable) {
    service.runLater(runnable);
  }


  public static String difference(final Moment start, final Moment finish) {
    return print(finish.millis() - start.millis());
  }
  public static String print(double millis) {
    if (millis < 0) {
      return "-" + print(-millis);
    }
    if (millis <= ONE_MINUTE) {
      return X_String.toMetricSuffix(millis/1000.0)+"sec";
    }
    int numPrinted = 0;
    StringBuilder b = new StringBuilder();
    if (millis > ONE_DAY) {
      int days =(int)(millis / ONE_DAY);
      b.append(days).append("D");
      numPrinted++;
    }
    int remain = ((int)millis) % ONE_DAY;
    int hours = remain / ONE_HOUR;
    if (hours > 0) {
        b.append(hours).append("h");
        remain %= ONE_HOUR;
        numPrinted++;
    }
    if (numPrinted > 1) {
      return b.toString();
    }
    int minutes = remain % ONE_HOUR;
    if (minutes > 0) {
      b.append(minutes / ONE_MINUTE).append("m");
      remain %= ONE_MINUTE;
        numPrinted++;
    }
    if (numPrinted > 1) {
      return b.toString();
    }
    int seconds = remain / 1000;
    if (seconds > 0) {
        b.append(seconds).append("s");
    }
    return b.toString();
  }
  public static Out1<String> diff(final Moment start) {
    final Moment now = now();
    return ()->difference(start, now);
  }
  public static String difference(final Moment start) {
    return difference(start, now());
  }

  public static void trySleep(final int millis, final int nanos) {
    assert millis > 0 || nanos > 0;
    if (Thread.interrupted()){
      Thread.currentThread().interrupt();
    } else {
      try {
        Thread.sleep(millis, nanos);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static double nowPlus(final double millis) {
    return now().millis()+millis;
  }

  public static Moment nowPlusOne() {
    return service.nowPlusOne();
  }

  public static boolean isPast(final double millis) {
    return millis < now().millis();
  }

  public static boolean isFuture(final double millis) {
    return millis > now().millis();
  }

  public static String timestamp() {
    return service.timestamp();
  }

  public static String timestamp(double millis) {
    return service.timestamp(millis);
  }

  public static String timestampHuman() {
    return service.timestampHuman();
  }

  public static String timestampHuman(double millis) {
    return service.timestampHuman(millis);
  }

  public static String printSinceBirth() {
    return print(now().millis() - birth());
  }
}
