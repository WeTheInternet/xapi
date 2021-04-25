package xapi.process.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ObjectTo;
import xapi.collect.init.AbstractMultiInitMap;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.Lazy;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.log.X_Log;
import xapi.process.api.*;
import xapi.process.api.ConcurrentEnvironment.Priority;
import xapi.process.api.Process;
import xapi.process.service.ConcurrencyService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Runtime;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static xapi.util.X_Debug.debug;

public abstract class ConcurrencyServiceAbstract implements ConcurrencyService{

  public static class TimeoutEntry {
    private final Do remove;
    private final WeakReference<Thread> thread;

    public TimeoutEntry(Do remove, Thread thread) {
      this.remove = remove;
      this.thread = new WeakReference<>(thread);
    }

    public Do getRemove() {
      return remove;
    }

    public WeakReference<Thread> getThread() {
      return thread;
    }
  }

  protected final EnviroMap environments = initMap();
  protected final Lazy<InterruptManager> interrupter;
  protected volatile boolean shutdown;

  private AtomicInteger threadCount = new AtomicInteger();

  protected ConcurrencyServiceAbstract() {
    interrupter = Lazy.deferred1(this::prepareInterrupter);
  }

  @Override
  public void shutdown() {
    shutdown = true;
    if (interrupter.isResolved()) {
      interrupter.out1().shutdown();
    }
  }

  @Override
  protected void finalize() throws Throwable {
  }

  protected InterruptManager prepareInterrupter() {
    ObjectTo.Many<Moment, Do> tasks = X_Collect.newMultiMap(Moment.class, Do.class,
        X_Collect.MUTABLE_CONCURRENT_KEY_ORDERED);
    class ThreadedThrowable extends RuntimeException {

      final Thread thread;

      ThreadedThrowable(Thread thread, Throwable cause) {
        super(cause);
        this.thread = thread;
      }
    }
    Thread interrupter = newThread(()->{
        while (true) {
          if (tasks.isEmpty()) {
            synchronized (tasks) {
              try {
                tasks.wait();
              } catch (InterruptedException e) {
                return;
              }
            }
          } else {
            tasks.removeWhileTrue((time, jobs)-> {
                if (X_Time.isFuture(time.millis())) {
                  return false;
                }
                ChainBuilder<ThreadedThrowable> failures = Chain.startChain();
                jobs.removeAll(job->{
                  try {
                    job.done();
                  } catch (Throwable t) {
                    if (t instanceof ThreadedThrowable) {
                      failures.add((ThreadedThrowable) t);
                    } else {
                      X_Log.error(ConcurrencyServiceAbstract.class,
                          "Interrupter callback ", job, " had unhandled error", t);
                    }
                  }
                });
                final UncaughtExceptionHandler handler =
                    X_Util.firstNotNull(
                    Thread.currentThread().getUncaughtExceptionHandler(),
                    Thread.getDefaultUncaughtExceptionHandler()
                );
                if (handler != null) {
                  failures.removeAll(failure->handler.uncaughtException(failure.thread, failure.getCause()));
                }
                return true;
            });
            if (!tasks.isEmpty()) {
              tasks.useFirstUnsafe(e->{
                long toWait = (long)e.out1().millis() - System.currentTimeMillis() + 1;
                if (toWait > 0) {
                  synchronized (tasks) {
                    tasks.wait(toWait);
                  }
                }
              });
            }
          }
        }
    });
    interrupter.setName("XApi Interrupter");
    interrupter.setDaemon(true);
    interrupter.start();
    return new InterruptManager(interrupter, tasks);
  }

  protected void prepareJob(Thread thread) {
    // Left here for subclasses to perform per-task init on a given thread
  }

  protected class EnviroMap extends
    AbstractMultiInitMap<Thread,ConcurrentEnvironment,UncaughtExceptionHandler> {

    public EnviroMap() {
      super(Thread::getName);
    }

    @Override
    protected ConcurrentEnvironment initialize(Thread key, UncaughtExceptionHandler params) {
      if (key.getState() == State.TERMINATED) {
        //send an exception...
        params.uncaughtException(key, new ThreadDeath());
      }
      if (key.isInterrupted()) {
        params.uncaughtException(key, new InterruptedException());
      }
      X_Log.trace(ConcurrencyServiceAbstract.class, "Initializing Concurrent Environment", key);
      final ConcurrentEnvironment inited = initializeEnvironment(key, params);
      inited.getThreads().forEach(t->
        environments.put(new AbstractPair<>(t, params), inited)
      );
      return inited;
    }
    @Override
    protected UncaughtExceptionHandler defaultParams() {
      return Thread.currentThread().getUncaughtExceptionHandler();
    }
  }

  protected abstract ConcurrentEnvironment initializeEnvironment(Thread key, UncaughtExceptionHandler params);

  protected int threadFlushTime() {
    return 2000;
  }

  @Override
  public Thread newThread(Do cmd, ThreadGroup group) {
    WrappedRunnable wrapped = wrap(cmd);
    Thread running = Thread.currentThread();
    ConcurrentEnvironment enviro = environments.get(running, running.getUncaughtExceptionHandler());
    final Runnable job = wrapped
        .doAfter(enviro::maybeShutdown).toRunnable();
    Thread childThread = group == null ? new Thread(job) : new Thread(group, job);
    childThread.setName(
        (group == null ? "" : group.getName()+"-") +
        cmd.getClass().getName()+"_"+threadCount.incrementAndGet()
    );
    enviro.pushThread(childThread);
    if (running.getUncaughtExceptionHandler() != null) {
      childThread.setUncaughtExceptionHandler(running.getUncaughtExceptionHandler());
    }
    return childThread;
  }

  /**
   * Allow all subclasses to wrap Runnables for custom behavior.
   * @param cmd - The supplied Runnable to execute.
   * @return - A wrapped Runnable, or cmd if not desired.
   *
   * This method is very useful for running benchmarks or testing assertions.
   */
  protected WrappedRunnable wrap(Do cmd) {
    if (cmd instanceof WrappedRunnable)
      return (WrappedRunnable)cmd;
    return new WrappedRunnable(cmd);
  }

  protected ConcurrentEnvironment currentEnvironment() {
    Thread running = Thread.currentThread();
    // TODO: have a reaper to monitor the calling thread, and clean up it's enviro when it's done running...
    return environments.get(running, running.getUncaughtExceptionHandler());
  }

  protected EnviroMap initMap() {
    return new EnviroMap();
  }

  @Override
  public <T> ProcessController<T> newProcess(Process<T> process) {
    return new ProcessController<T>(process);
  }

  @Override
  public <T> void resolve(final Future<T> future, final In1<T> receiver, In1Unsafe<Throwable> failure) {
    if (future.isDone()) {
      callback(future, receiver, failure);
      return;
    }
    //The future isn't done.  Let's push a task into the enviro.
    Thread otherThread = getFuturesThread();
    ConcurrentEnvironment enviro = environments.get(otherThread, otherThread.getUncaughtExceptionHandler());
    enviro.monitor(Priority.Low, future::isDone,
        () -> callback(future, receiver, failure)
    );
  }

  /**
   * Allows multi-threaded environments to have a single thread dedicated to
   * monitoring futures for completion.
   * @return - The thread to be used for monitoring futures
   */
  protected Thread getFuturesThread() {
    return Thread.currentThread();
  }

  protected <T> void callback(Future<T> future, In1<T> receiver, In1<Throwable> failure) {
    callback(future, receiver, error-> {
      debug(error);
      X_Util.rethrow(X_Util.unwrap(error));
    });
  }

  protected <T> void callback(Future<T> future, In1<T> receiver, In1Unsafe<Throwable> failure) {
    try {
      receiver.in(future.get());
      return;
    } catch (InterruptedException e) {
      Thread.interrupted();
      failure.in(e);
    } catch (ExecutionException e) {
      failure.in(e);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean kill(Thread thread, int timeout) {
    if (finishJob(thread, timeout))
      return true;
    try {
      thread.interrupt();
      return false;
    }catch (Exception e) {
      thread.stop();
      return false;
    }
  }

  protected boolean finishJob(Thread thread, int timeout) {
    if (environments.hasKey(thread.getName())) {
      ConcurrentEnvironment enviro = environments.get(thread, thread.getUncaughtExceptionHandler());
      boolean success = enviro.destroy(timeout);
      environments.removeValue(thread.getName());
      return success;
    }
    return true;
  }

  @Override
  public boolean trySleep(float millis) {
    if (Thread.interrupted())
      return false;
    float leftover = millis - ((int)millis);
    try {
      Thread.sleep((long)millis, (int)(leftover * 1000000));
      return true;
    }catch (InterruptedException e) {
      Thread.interrupted();
      return false;
    }
  }

  @Override
  public boolean flush(Thread thread, int timeout) {
    ConcurrentEnvironment enviro = environments.getValue(thread.getName());
    if (thread == Thread.currentThread()) {
      if (enviro == null)
        return true;// nothin' to do here!
      long deadline = System.currentTimeMillis()+timeout;
      while (enviro.flush((int)(deadline-System.currentTimeMillis()))) {
        int timeLeft = (int)(deadline-System.currentTimeMillis());
        if (timeLeft<1)
          return false;
        //join a thread, if available
        Iterator<Thread> iter = enviro.getThreads().iterator();
        try {
          Thread next;
          synchronized (enviro) {
            if (iter.hasNext()) {
              next = iter.next();
              iter.remove();
            }else
              return true;
          }

        if (next != null)
          next.join(timeLeft);
        } catch (InterruptedException e) {
          finishJob(Thread.currentThread(), timeLeft);
          Thread.currentThread().interrupt();
        }
        if (System.currentTimeMillis()>deadline) {
          System.out.println("Not done when deadline was hit");
          return false;
        }
      }
      return true;
    }else {
      if (enviro != null)
        enviro.scheduleFlush(timeout);
      return false;
    }
  }

  @Override
  public double now() {
    return System.currentTimeMillis();
  }

  @Override
  public double threadStartTime(Thread thread) {
    return environments.get(thread, thread.getUncaughtExceptionHandler()).startTime();
  }

  public boolean isMultiThreaded() {
    return X_Runtime.isMultithreaded();
  }


  @Override
  public void runDeferred(Do cmd) {
    ConcurrentEnvironment enviro = currentEnvironment();
    enviro.pushDeferred(cmd);
  }

  @Override
  public void runEventually(Do cmd) {
    ConcurrentEnvironment enviro = currentEnvironment();
    enviro.pushEventually(cmd);
  }

  @Override
  public void runFinally(Do cmd) {
    ConcurrentEnvironment enviro = currentEnvironment();
    enviro.pushFinally(cmd);
  }

  @Override
  public void runInClassloader(ClassLoader loader, Do cmd) {
    if (Thread.currentThread().getContextClassLoader() == loader) {
      runDeferred(cmd);
    } else {
      // TODO: somehow / optionally encapsulate X_Scope.currentScope to survive in foreign classloader.
      // If implemented, this should enforce parent classloaders,
      // or some hideous thing to try to make reflection proxies of "everything dumped into scope".
      if (loader instanceof HasThreadGroup) {
        final ThreadGroup group = ((HasThreadGroup) loader).getThreadGroup();
        if (group != null) {

        }
      } else {

      }
      final Thread thread = newThread(cmd);
      thread.setContextClassLoader(loader);
      thread.start();
    }
  }

  @Override
  public Do scheduleInterruption(long blocksFor, TimeUnit unit) {
    return interrupter.out1().requestInterruption(Thread.currentThread(), blocksFor, unit).getRemove();
  }

  protected class WrappedRunnable implements Do {

    private Do core;

    public WrappedRunnable(Do core) {
      this.core = core;
    }

    @Override
    public void done() {
      prepareJob(Thread.currentThread());
      core.done();
      finishJob(Thread.currentThread(), threadFlushTime());

      //Now that we've finished the job we were told to do,
      //let's attempt to reuse our current thread


      //We should choose how to "steal work" wisely,
      //using an algorithm which has known ETA times,
      //so a thread which knows about an upcoming task
      //can choose to reject long-running tasks;

      //This will require coordination around the workload of other threads.
      //If there is already one thread spinning, looking for work,
      //and that thread is spending less than X % of time working,
      //then we can take on any job.

      //There will also be a necessary priority calculation;
      //A big pending high priority job should take any live threads,
      //and we can spin up more replacements for other tasks if needed.

      //In order to prevent attention-starvation, a task's priority will get
      //bumped up if it has to wait too long.

      //Using a min-max latency and eta will help in determining the best job
      //to take.

      //First, check for immediate jobs scheduled by the current thread.
      //this will help in cases when a thread simply wants to yield,
      //or when a forking process wants to continue immediately,
      //or when gluing together methods into a process.


      //Next, check for high priority jobs scheduled by any thread.
      //Anything with a timeout at or near expiration should be taken.
      //If there are known tiny jobs available with a known eta less than
      //the wait time for the high priority job, that job should be taken as well.
      //If no such jobs exist, scan the work queue to elevate any starving tasks

      //If no high priority jobs are waiting, scan the work queue for either
      //a) work to do; if timeout ~expired, take and run immediately
      //b) tasks to elevate;
      //the iterator supplied when looking for work should elevate on its own.

      //if no task is found, die unless you are the last thread.
      //if timeout > now, drain the iterator to make sure anything needing
      //elevation gets it.
    }

  }

}
