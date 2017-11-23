package xapi.process.service;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.api.DoNotOverride;
import xapi.process.api.AsyncLock;
import xapi.process.api.Process;
import xapi.process.api.ProcessController;
import xapi.time.service.TimeService;
import xapi.util.api.ErrorHandler;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ConcurrencyService {

  void shutdown();

  /**
   * Experimental; may be deprecated.
   * Do not override this method; instead override {@link #newThread(Do, ThreadGroup)}.
   *
   * Creates a new thread for the given runnable.
   *
   * If blacklisting Thread is a problem on some platforms,
   * Thread may be replaced with a proxy class in the public api.
   *
   * @param cmd - The command to wrap in a new thread.
   * @return - An unstarted thread that will run the given command.
   *
   */
  @DoNotOverride
  default Thread newThread(Do cmd) {
    return newThread(cmd, null);
  }

  /**
   * Experimental; may be deprecated.
   *
   * Creates a new thread for the given runnable.
   *
   * If blacklisting Thread/ThreadGroup is a problem on some platforms,
   * Thread may be replaced with a proxy class in the public api.
   *
   * @param cmd - The command to wrap in a new thread.
   * @param group - The threadgroup you want the thread to belong to
   * @return - An unstarted thread that will run the given command.
   */
  Thread newThread(Do cmd, ThreadGroup group);

  /**
   * Experimental; may replace {@link #newThread(Do)}.
   *
   * Accepts an instance of {@link Process}, and returns a {@link ProcessController}.
   *
   * Designed to give an abstract action-oriented workflow that functions in
   * both threadsafe and singlethreaded environments.
   *
   * @param process
   * @return
   */
  <T> ProcessController<T> newProcess(Process<T> process);

  /**
   * Runs the given command in a new environment, after all finalies have been
   * processed.
   * @param cmd - The task to run.
   */
  void runDeferred(Do cmd);
  /**
   * Runs the given command after the work queue for the current environment
   * is drained.  Finalies must always be cleared before a given unit of
   * work is truly "finished".  You may safely schedule multiple layers of
   * finally blocks from other finally blocks; the queue is copied and reset
   * for every iteration.  Just beware that this enforces "single-threadedness",
   * and will cause blocking in a concurrent environment.  In GWT, it will not
   * release the processing thread for UI updates until all finalies are complete.
   *
   * @param cmd - The command to run after the work queue is drained.
   */
  void runFinally(Do cmd);
  /**
   * Wait (at least) a given number of milliseconds to perform a task.
   *
   * Exact timing promises are not made, so if you need an exact timing in
   * a concurrent environment, schedule your deadline 10-50 millis early,
   * or even better, track latency (how much later you are called then requested)
   * and adjust your timeouts accordingly.
   *
   * @param cmd - The command to run.
   * @param millisToWait - A minimum of milliseconds to wait.
   */
  void runTimeout(Do cmd, int millisToWait);
  /**
   * Runs the command eventually, most likely on an unused thread.
   *
   * This is good for cleaning up memory or doing other house-keeping chores
   * only when there isn't any other work to do.
   *
   * Each implementation may handle eventualies different, and no guarantee is
   * made about what order your command is run (it could be run immediately in
   * a parallel thread, or it could wait a few minutes until a thread-hungry
   * process is complete, and get cleared by starving threads before they are
   * released.
   *
   * @param cmd - A command to run whenever there is time.
   *
   * No guarantee is made about how soon or how later the process is run.
   * A best-effort is made to use warm threads with nothing better to do
   * to process eventually commands.
   */
  void runEventually(Do cmd);

  /**
   * Resolves a future in another thread / using a shared poller.
   *
   * In concurrent environments allowed many threads, resolve will
   * dedicate a single thread to wait on the future; but in a starved environment,
   * a single thread will be used to check for completed futures, dispatch
   * callbacks, sleep a little, and "busy wait" on all pending futures.
   *
   * (this functionality is not yet implemented).
   *
   * @param future - The future to wait upon.
   * @param receiver - The handler to receive the value.
   *
   * If received is an instanceof {@link ErrorHandler}, it will receive
   * any exceptions caused by the future (after unwrapping ExecutionException
   * and RuntimeException).
   */
  <T> void resolve(Future<T> future, In1<T> receiver);

  /**
   * Flushes all jobs for a given thread, up to a given millisecond timeout.
   *
   * This attempts to clear all jobs registered for the given thread, and waits
   * a maximum of timeout milliseconds.  All deferred and finally tasks will
   * be cleared, and eventualies will be run if there is time to bother.
   *
   * This method blocks / busy waits until the work is complete
   * or the timeout is exceeded.
   *
   * @param thread - The thread to finish.
   * @param timeout - The maximum time to take. Send 0 to wait indefinitely.
   * @return - true is all work (except eventuallies) was completed.
   */
  boolean flush(Thread thread, int timeout);
  /**
   * Flushes all jobs for the given thread, and then kills it.
   *
   * A killed thread will not receive any new work if asks for it,
   * so draining its task queue and denying it work will shut it down.
   *
   * The thread will be manually interrupted after timeout milliseconds.
   *
   * @param thread - The thread to finish and then kill.
   * @param timeout - How many milliseconds to wait for a graceful shutdown.
   * @return - true if the thread shutdown gracefully.
   *
   * Once the timeout is passed, the running thread will interrupt the given
   * thread, and then return false.
   */
  boolean kill(Thread thread, int timeout);

  /**
   * Attempts to sleep, if the current thread is not interrupted.
   *
   * Experiments with rewriting AST in GWT may make "pseudo-sleep" possible,
   * by travelling up the method call to the nearest method with void return type,
   * and moving all code into a scoped callback function.
   *
   * Such experiments will not likely yield useful rewards, but will be carried
   * out anyway.  Until then, sleep is a no-op in GWT.
   *
   * When the GwtThread class is complete, calling sleep will tell the
   * ConcurrencyService to stop running tasks from this thread until the given
   * timeout is expired.
   *
   * @param millis - The millis to wait.  Floating point converts to nanos.
   * @return - true is wait was successful and we are not in an interrupted state.
   */
  boolean trySleep(float millis);

  /**
   * Tells what time the current process for the current thread started.
   *
   * The thread itself may be much older, if it has stayed busy enough to
   * avoid starvation.
   *
   * @param currentThread - The thread who's start time to check.
   * @return - nanos since now() that the thread started.
   */
  double threadStartTime(Thread currentThread);
  /**
   * @return Current nano-time, since {@link TimeService#birth()}.
   */
  double now();

  /**
   * @return A lock suitable for the environment in which you are running.
   *
   * We may extend the Lock interface, and require subclasses that offer
   * a given, small set of additional functionality.
   */
  AsyncLock newLock();

    boolean isInProcess();

    void runInClassloader(ClassLoader loader, Do cmd);

    void scheduleInterruption(long blocksFor, TimeUnit unit);

}
