package xapi.gwt.process;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.fifo.Fifo;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.gwt.collect.JsFifo;
import xapi.log.X_Log;
import xapi.platform.GwtPlatform;
import xapi.process.api.AsyncCondition;
import xapi.process.api.AsyncLock;
import xapi.process.api.ConcurrentEnvironment;
import xapi.process.impl.ConcurrencyServiceAbstract;
import xapi.process.service.ConcurrencyService;
import xapi.util.api.ErrorHandler;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.user.client.Timer;

import java.lang.Thread.UncaughtExceptionHandler;

@GwtPlatform
@SingletonOverride(implFor=ConcurrencyService.class)
public class ConcurrencyServiceGwt extends ConcurrencyServiceAbstract{

  private static final AsyncLock NO_OP = new SingleThreadedLock();

  class GwtEnvironment extends ConcurrentEnvironment {

    private final Fifo<Do> defers = JsFifo.newFifo();
    private final Fifo<Do> finalies = JsFifo.newFifo();
    private final Fifo<Do> eventualies = JsFifo.newFifo();
    private final Fifo<Thread> threads = JsFifo.newFifo();

    @Override
    public Iterable<Thread> getThreads() {
      return threads.forEach();
    }

    @Override
    public Iterable<Do> getDeferred() {
      return defers.forEach();
    }

    @Override
    public Iterable<Do> getFinally() {
      return finalies.forEach();
    }

    @Override
    public void pushDeferred(Do cmd) {
      defers.give(cmd);
    }

    @Override
    public void pushFinally(Do cmd) {
      finalies.give(cmd);
    }

    @Override
    public void pushThread(Thread childThread) {
      threads.give(childThread);
    }

    @Override
    public void pushEventually(Do cmd) {
      eventualies.give(cmd);
    }
  }

  @Override
  public boolean isInProcess() {
    return Impl.isEntryOnStack();
  }

  @Override
  protected ConcurrentEnvironment initializeEnvironment(Thread key, UncaughtExceptionHandler params) {
    return new GwtEnvironment();
  }

  @Override
  public void runDeferred(final Do cmd) {
    Scheduler.get().scheduleDeferred(cmd::done);
  }

  @Override
  public void runFinally(final Do cmd) {
    Scheduler.get().scheduleFinally(cmd::done);
  }

  @Override
  public void runTimeout(final Do cmd, int millisToWait) {
    new Timer() {
      @Override
      public void run() {
        cmd.done();
      }
    }.schedule(millisToWait);
  }

  @Override
  public void runEventually(Do cmd) {
    currentEnvironment().pushEventually(cmd);
  }

  @Override
  public AsyncLock newLock() {
    return NO_OP;
  }

  @Override
  public boolean trySleep(float millis) {
    //push a sleep command onto stack.
    //Note that it won't cause execution to stop,
    //but it will delay how long until the thread is called again.
    return false;
  }

  @Override
  public double now() {
    return Duration.currentTimeMillis();
  }

}

class SingleThreadedLock implements AsyncLock {

  private boolean locked;
  private Fifo<SuccessHandler<AsyncLock>> pending = JsFifo.newFifo();

  @Override
  public AsyncCondition newCondition() {
    throw new NotYetImplemented("AsyncCondition not yet implemented");
  }

  @Override
  public boolean tryLock() {
    if (locked) {
      return false;
    }
    return (locked = true);
  }

  @Override
  public RemovalHandler lock(final SuccessHandler<AsyncLock> onLocked) {
    pending.give(onLocked);
    return new RemovalHandler() {
      @Override
      public void remove() {
        pending.remove(onLocked);
      }
    };
  }

  @Override
  @SuppressWarnings({"rawtypes","unchecked"})
  public void unlock() {
    locked = false;
    // Pull task off queue and do it too.
    if (pending.isEmpty())
      return;
    SuccessHandler<AsyncLock> handler = pending.take();
    try {
      handler.onSuccess(this);
    } catch (Throwable e) {
      if (handler instanceof ErrorHandler) {
        try {
          ((ErrorHandler)handler).onError(e);
        } catch (Exception ignored) {}
      } else {
        X_Log.warn("Error in AsyncLock.unlock() for "+getClass(), e);
      }
    }
  }
};

