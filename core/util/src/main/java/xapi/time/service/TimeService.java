package xapi.time.service;

import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;

public interface TimeService extends Moment{

  Double nano_to_milli = 0.000001;

  /**
   * creates an immutable copy of a moment
   * @param moment - The moment to copy
   * @return an {@link ImmutableMoment} that == moment
   */
  Moment clone(Moment moment);
  /**
   * Updates .now() to a value that is at least as high as the current system time,
   * and always higher than any previous value returned from .now()
   *
   * When this is invoked multiple times within the same millisecond, implementations must
   * return a double value that is uniquely higher than the current second, but ideally as small as possible.
   *
   * In the worst case scenario, now() + Math.ulp(System.currentTimeMillis) will leave 0x800 spaces between millis,
   * and this worst case happens at Wed Sep 07 2039 08:47:35 GMT-7,
   * which is the millisecond timestamp 0x200_0000_0000 (uses the most bits in significand).
   *
   * Until then, we can safely rely on 0x1000 spaces between milliseconds.
   *
   */
  double tick();

  double lastTick();
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
   * A generic means to defer a task until later;
   * if you need non-trivial scheduling,
   * consider inheriting xapi-core-process (and the platform-specific implementation you need).
   *
   * This is useful to avoid depending on Thread directly;
   * The default implementation is quite sloppy;
   * it just creates and starts a thread with your runnable.
   *
   * The Gwt version uses the deferred scheduler,
   * and appengine uses it's blessed thread factory.
   *
   * You are highly encouraged to extend the time service for your platform,
   * and put in some better task scheduling here.
   *
   * TODO: return a Future&lt;> or other means to cancel / control the task
   *
   */
  void runLater(Runnable runnable);

  /**
   * @return An ISO-8601 compliant timestamp:
   * yyyy-MM-dd'T'HH:mm.sssZ
   */
  String timestamp();


}
