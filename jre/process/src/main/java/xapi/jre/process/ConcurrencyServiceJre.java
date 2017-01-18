package xapi.jre.process;

import xapi.annotation.inject.SingletonDefault;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.platform.JrePlatform;
import xapi.process.api.AsyncCondition;
import xapi.process.api.AsyncLock;
import xapi.process.api.ConcurrentEnvironment;
import xapi.process.impl.ConcurrencyServiceAbstract;
import xapi.process.service.ConcurrencyService;
import xapi.util.X_Namespace;
import xapi.util.api.ErrorHandler;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import static xapi.fu.Lazy.deferred1;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

@JrePlatform
@SingletonDefault(implFor=ConcurrencyService.class)
public class ConcurrencyServiceJre extends ConcurrencyServiceAbstract{

  public ConcurrencyServiceJre() {
  }

  protected class JreConcurrentEnvironment extends ConcurrentEnvironment {

    private final Lazy<Queue<Do>> defers = deferred1(ConcurrentLinkedQueue::new);
    private final Lazy<Queue<Do>> finalies = deferred1(ConcurrentLinkedQueue::new);
    private final Lazy<Queue<Do>> eventualies = deferred1(ConcurrentLinkedQueue::new);
    private final Lazy<Queue<Thread>> threads = deferred1(ConcurrentLinkedQueue::new);


    @Override
    public Iterable<Do> getDeferred() {
      return defers.out1();
    }

    @Override
    public Iterable<Thread> getThreads() {
      return threads.out1();
    }

    @Override
    public Iterable<Do> getFinally() {
      return finalies.out1();
    }

    @Override
    public void pushDeferred(Do cmd) {
      defers.out1().add(cmd);
    }

    @Override
    public void pushEventually(Do cmd) {
      eventualies.out1().add(cmd);
    }

    @Override
    public void pushFinally(Do cmd) {
      finalies.out1().add(cmd);
    }

    @Override
    public void pushThread(Thread cmd) {
      threads.out1().add(cmd);
    }

    @Override
    public boolean isEmpty() {
      if (defers.isResolved()) {
        if (!defers.out1().isEmpty())
          return false;
      }
      if (finalies.isResolved()) {
        return finalies.out1().isEmpty();
      }
      return true;
    }
  }

  private final SingletonProvider<Integer> maxThreads = new SingletonProvider<Integer>() {
    @Override
    protected Integer initialValue() {
      return Integer.parseInt(System.getProperty(X_Namespace.PROPERTY_MULTITHREADED, "5"));
    }
  };

  @Override
  public ConcurrentEnvironment initializeEnvironment(
    Thread key, UncaughtExceptionHandler params) {
    ConcurrentEnvironment enviro = new JreConcurrentEnvironment();
    Mutable<Integer> delay = new Mutable<>(50_000);
    final Thread thread = createThread(() -> {
      while (true) {
        if (enviro.flush(10)) {
          LockSupport.parkNanos(delay.out1());
          // Cap delay at 1/4 of a second
          delay.in(Math.max(250_000_000, delay.out1() * 3/2));
        } else {
          delay.in(50_000_000);
        }
      }
    });
    enviro.pushThread(thread);
    thread.setDaemon(true);
    thread.start();

    return enviro;
  }

  protected Thread createThread(Do task) {
    return new Thread(task.toRunnable());
  };

  protected int maxThreads() {
    return maxThreads.get();
  }

  @Override
  public boolean isMultiThreaded() {
    return maxThreads() > 1;
  }

  @Override
  public void runTimeout(Do cmd, int millisToWait) {

  }


  @Override
  public AsyncLock newLock() {
    return new LockWrapper();
  }

}

class LockWrapper implements AsyncLock {

  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public AsyncCondition newCondition() {
    throw new NotYetImplemented("AsyncCondition not yet implemented");
  }

  @Override
  public boolean tryLock() {
    return lock.tryLock();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public RemovalHandler lock(SuccessHandler<AsyncLock> onLocked) {
    // TODO actually push the callback onto a deferred stack,
    // and return without blocking.
    try {
      lock.lock();
      onLocked.onSuccess(this);
    } catch (Throwable e){
      if (onLocked instanceof ErrorHandler) {
        try {
          ((ErrorHandler)onLocked).onError(e);
        } catch (Throwable ignored) {}
      } else {
        X_Log.warn("Error occured while performing lock callback on "+onLocked,
          onLocked);
      }
    }
    return RemovalHandler.DoNothing;
  }

  @Override
  public void unlock() {
    lock.unlock();
  }



}
