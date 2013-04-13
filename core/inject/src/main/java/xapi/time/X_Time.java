package xapi.time;

import static xapi.inject.X_Inject.singleton;
import xapi.time.api.Moment;
import xapi.time.api.TimeService;
import xapi.time.impl.ImmutableMoment;
import xapi.util.X_String;

public class X_Time {

  private X_Time() {}
  
  private static final TimeService
      service = singleton(TimeService.class);


  public static Moment now() {
    return service.now();
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
   * Advances {@link #now()} to current time.
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
  public static Moment clone(Moment moment) {
    return service.clone(moment);
  }

  /**
   * @return Milliseconds since epoch
   * when the static timeservice was instantiated (X_Time first accessed).
   */
  public static double birth() {
    return service.birth();
  }

  public static void runLater(Runnable runnable) {
    service.runLater(runnable);
  }

  public static String difference(Moment start, Moment finish) {
    return X_String.toMetricSuffix((finish.millis() - start.millis())/1000.0)+"seconds";
  }
  public static String difference(Moment start) {
    return difference(start, now());
  }

  public static void trySleep(int millis, int nanos) {
    assert millis > 0 || nanos > 0;
    if (Thread.interrupted()){
      Thread.currentThread().interrupt();
    }
    else
    try {
      Thread.sleep(millis, nanos);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static double nowPlus(double millis) {
    return now().millis()+millis;
  }

  public static boolean isPast(double millis) {
    return millis < now().millis();
  }

  public static boolean isFuture(double millis) {
    return millis > now().millis();
  }
}
