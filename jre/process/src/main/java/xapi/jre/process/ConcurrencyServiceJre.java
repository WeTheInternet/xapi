package xapi.jre.process;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import xapi.annotation.inject.SingletonDefault;
import xapi.except.NotYetImplemented;
import xapi.inject.impl.LazyPojo;
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

@JrePlatform
@SingletonDefault(implFor=ConcurrencyService.class)
public class ConcurrencyServiceJre extends ConcurrencyServiceAbstract{

  public ConcurrencyServiceJre() {
  }

  private static class LazyQueue<T> extends LazyPojo<ConcurrentLinkedQueue<T>> {
    @Override
    protected ConcurrentLinkedQueue<T> initialValue() {
      return new ConcurrentLinkedQueue<T>();
    }
  }

  protected class JreConcurrentEnvironment extends ConcurrentEnvironment {

    private final LazyQueue<Runnable> defers = new LazyQueue<Runnable>();
    private final LazyQueue<Runnable> finalies = new LazyQueue<Runnable>();
    private final LazyQueue<Runnable> eventualies = new LazyQueue<Runnable>();
    private final LazyQueue<Thread> threads = new LazyQueue<Thread>();

    @Override
    public Iterable<Runnable> getDeferred() {
      return defers.get();
    }

    @Override
    public Iterable<Thread> getThreads() {
      return threads.get();
    }

    @Override
    public Iterable<Runnable> getFinally() {
      return finalies.get();
    }

    @Override
    public void pushDeferred(Runnable cmd) {
      defers.get().add(cmd);
    }

    @Override
    public void pushEventually(Runnable cmd) {
      eventualies.get().add(cmd);
    }

    @Override
    public void pushFinally(Runnable cmd) {
      finalies.get().add(cmd);
    }

    @Override
    public void pushThread(Thread cmd) {
      threads.get().add(cmd);
    }

    @Override
    public boolean isEmpty() {
      if (defers.isSet()) {
        if (!defers.get().isEmpty())
          return false;
      }
      if (finalies.isSet()) {
        return finalies.get().isEmpty();
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
    return enviro;
  };

  protected int maxThreads() {
    return maxThreads.get();
  }

  @Override
  public boolean isMultiThreaded() {
    return maxThreads() > 1;
  }

  @Override
  public void runTimeout(Runnable cmd, int millisToWait) {

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
