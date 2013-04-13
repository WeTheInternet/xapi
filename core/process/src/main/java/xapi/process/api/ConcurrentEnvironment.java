package xapi.process.api;

import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import javax.inject.Provider;

import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.util.X_Debug;
import static xapi.process.X_Process.now;

public abstract class ConcurrentEnvironment {

  public static enum Priority {
    High, Medium, Low
  }
  public static enum Strategy {
    Block, FixedPeriod, DecayingPeriod, Recycle
  }

  private final Object synchro = new Object();

  private static final double start = X_Process.now();

  public void monitor(Priority priority, Provider<Boolean> gate, Runnable job) {

  }

  public boolean hasFinalies() {
    return getFinally().iterator().hasNext();
  }
  public boolean hasDefers() {
    return getDeferred().iterator().hasNext();
  }

  public boolean isEmpty() {
    return (!(hasFinalies()||!hasDefers()));
  }

  public boolean flush(int timeout) {
    if (isEmpty())
      return true;
    double max = now()+timeout;
    try {
    do {
      if (hasFinalies()) {
        System.out.println("run finallies");
        runFinalies(max);
      }
      System.out.println("check timeout");
      checkTimeouts(max);

      Iterator<Runnable> iter = getDeferred().iterator();
      while (iter.hasNext()) {
        System.out.println("iterating job");
        Runnable next;
        synchronized (synchro) {
          next = iter.next();
          iter.remove();
        }
        next.run();
        System.out.println("check finalies again");
        if (hasFinalies()) {
          System.out.println("has finalies");
          runFinalies(max);
        }
        System.out.println("check timeout");
        checkTimeouts(max);
      }
    }while(! isEmpty());
    }catch (TimeoutException e) {
      return false;
    }finally {
      if (X_Debug.isBenchmark()) {
        X_Log.info("Spent "+(now()-(max-timeout))+" millis flushing environment");
      }
    }
    return false;
  }

  protected void checkTimeouts(double max) throws TimeoutException {
    if (now() > max)
      throw new TimeoutException();
  }

  protected void runFinalies(double max) throws TimeoutException {
    Iterator<Runnable> iter = getFinally().iterator();
    while (iter.hasNext()) {
      Runnable next;
      synchronized (synchro) {
        next = iter.next();
        iter.remove();
      }
      next.run();
      if (now() > max)
        throw new TimeoutException();
    }
  }

  public void scheduleFlush(int timeout) {

  }

  public boolean destroy(int timeout) {
    double max = now() + timeout;
    while (!isEmpty()) {
      int left = (int)(max - now());
      if (left < 1)
        return true;
      flush(left);
    }
    return false;
  }

  public double startTime() {
    return start;
  }

  public abstract Iterable<Runnable> getDeferred();
  public abstract Iterable<Runnable> getFinally();
  public abstract Iterable<Thread> getThreads();

  public abstract void pushDeferred(Runnable cmd);
  public abstract void pushEventually(Runnable cmd);
  public abstract void pushFinally(Runnable cmd);
  public abstract void pushThread(Thread childThread);


}
