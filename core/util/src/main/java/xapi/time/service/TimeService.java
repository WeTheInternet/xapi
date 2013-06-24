package xapi.time.service;

import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;

public interface TimeService extends Moment{

  final Double second_to_nano = 0.000000001;
  final Double milli_to_nano = 0.000001;

  //float now();// from Moment

  /**
   * creates an immutable copy of a moment
   * @param moment - The moment to copy
   * @return an {@link ImmutableMoment} that == moment
   */
  Moment clone(Moment moment);
  /**
   * updates now() to current time
   */
  void tick();
  /**
   * the epoch in millis when this TimeService was instantiated
   * @return - System startup time.
   */
  double birth();

  Moment now();
  /**
   * Returns a new moment guaranteed to be one nano later than any other method.
   * We use a volatile delta float which we check-and-increment for each call
   * to nowPlusOne().
   *
   * This is useful if you want to have usable map keys which point to the same
   * millis timeslow, but have atomic "pseudo nano precision" on systems with
   * low resolution timers.
   *
   * This allows you to use a single ordered queue without resorting to buckets
   * to hold items scheduled at the same time.
   *
   * High resolution time services will use correct nano precision,
   * at the cost of synchronizing on calls to System.nanoTime(),
   * to ensure unique timestamps across all calls to the time service.
   *
   * @return A guaranteed unique timestamp gauranteed later than now()
   * until the next call to tick();
   */
  Moment nowPlusOne();
  /**
   * Attempts to sleep if the current thread is not already interrupted.
   *
   * This method will reset the interrupted flag if the thread is interrupted
   * while sleeping.
   *
   * @param millis - Millis to wait, >= 0
   * @param nanos - Nanos to wait, must be > 0 is millis == 0
   */
//  void trySleep(int millis, int nanos);
  void runLater(Runnable runnable);
  
  /**
   * @return An ISO-8601 compliant timestamp:
   * yyyy-MM-dd'T'HH:mm.sssZ
   */
  String timestamp();


}
