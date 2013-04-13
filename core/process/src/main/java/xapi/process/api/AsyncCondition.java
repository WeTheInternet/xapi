package xapi.process.api;

import java.util.concurrent.CancellationException;

import xapi.util.api.ErrorHandler;
import xapi.util.api.Pair;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

/**
 * In order to ease the apis of code shared with single-threaded environments,
 * we create an Async abstraction that can function efficiently in a threadsafe
 * enviro, while giving a useful "illusion of concurrency" when single-threaded.
 *
 * It does force a callback pattern onto code that may otherwise have been
 * able to simple block and run in a nice procedural fashion,
 * but it enables a more functional approach, that will scale much better.
 *
 * You do not wait for your work to be finished, you send the rest of work,
 * and it is run for you when the given condition is satisfied
 * (the owner of the lock calls {@link #signal()}.)
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface AsyncCondition {

  /**
   * Waits indefinitely for the given condition.
   *
   * @param onAcquire - A SuccessHandler to be notified when we are signalled.
   * @return - A removal handler to cancel the wait if it hasn't already completed.
   *
   * If you want a deadline, use {@link #awaitWithDeadline(SuccessHandler, float)}
   * If your SuccessHandler is also an {@link ErrorHandler}, you will receive
   * a {@link CancellationException} when RemovalHandler.remove() is called.
   */
  RemovalHandler await(SuccessHandler<Pair<AsyncLock, AsyncCondition>> onAcquire);

  /**
   * The same rules as {@link #await(SuccessHandler)}, but with a millisecond
   * timeout that will share a notification pool, so a minimum of threads
   * are needed to simply check deadlines and cancel / notify / continue.
   *
   * @param onAcquire - The success handler to call when we are signalled.
   * @param millisToWait - A floating point number of milliseconds to wait.
   * @return A {@link RemovalHandler} to cancel the operation.
   * Does nothing if signal has already been called.
   */
  RemovalHandler awaitWithDeadline(SuccessHandler<Pair<AsyncLock, AsyncCondition>> onAcquire
    , float millisToWait);

  /**
   * Signals one waiting success handler, and returns true,
   * -if the pool of waiting conditions was empty BEFORE calling onAcquire.onSuccess-
   *
   * @return - True is the waiting pool on SuccessHandlers had, at most one item
   * in it when signal() is called.  This can allow a flushing method to safely
   * call while(signal()); to drain a condition.
   */
  boolean signal();
}
