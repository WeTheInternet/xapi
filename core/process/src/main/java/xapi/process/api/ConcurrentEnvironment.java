package xapi.process.api;

import xapi.fu.Do;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.util.X_Debug;

import static xapi.process.X_Process.now;

import javax.inject.Provider;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

public abstract class ConcurrentEnvironment {

  public enum Priority {
    High, Medium, Low
  }
  public enum Strategy {
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

      Iterator<Do> iter = getDeferred().iterator();
      while (iter.hasNext()) {
        System.out.println("iterating job");
        Do next;
        synchronized (synchro) {
          next = iter.next();
          iter.remove();
        }
        next.done();
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
    Iterator<Do> iter = getFinally().iterator();
    while (iter.hasNext()) {
      Do next;
      synchronized (synchro) {
        next = iter.next();
        iter.remove();
      }
      next.done();
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

  public abstract Iterable<Do> getDeferred();
  public abstract Iterable<Do> getFinally();
  public abstract Iterable<Thread> getThreads();

  public abstract void pushDeferred(Do cmd);
  public abstract void pushEventually(Do cmd);
  public abstract void pushFinally(Do cmd);
  public abstract void pushThread(Thread childThread);


}
