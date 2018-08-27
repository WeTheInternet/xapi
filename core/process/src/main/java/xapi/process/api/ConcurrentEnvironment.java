package xapi.process.api;

import xapi.fu.Do;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.util.X_Debug;

import javax.inject.Provider;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import static xapi.process.X_Process.now;

public abstract class ConcurrentEnvironment {

  public enum Priority {
    High, Medium, Low
  }
  public enum Strategy {
    Block, FixedPeriod, DecayingPeriod, Recycle
  }

  private final Object synchro = new Object();

  private final double start = X_Process.now();
  private double touched = start;
  protected boolean stop;
  // Enviros will stay alive when empty, by default, for 5s.
  private double TTL = Double.parseDouble(System.getProperty("xapi.process.ttl", "5000"));

    public void monitor(Priority priority, Provider<Boolean> gate, Runnable job) {
    pushDeferred(()->{
      if (Boolean.TRUE.equals(gate.get())) {
        job.run();
      } else {
        monitor(priority, gate, job);
      }
    });
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
        X_Log.debug(ConcurrentEnvironment.class, "run finallies");
        runFinalies(max);
      }
      X_Log.debug(ConcurrentEnvironment.class, "check timeout");
      checkTimeouts(max);

      Iterator<Do> iter = getDeferred().iterator();
      while (iter.hasNext()) {
        Do next;
        synchronized (synchro) {
          next = iter.next();
          iter.remove();
        }
        X_Log.trace(ConcurrentEnvironment.class, "iterating job", next);
        next.done();
        X_Log.debug(ConcurrentEnvironment.class, "check finalies again");
        if (hasFinalies()) {
          X_Log.trace(ConcurrentEnvironment.class, "has finalies");
          runFinalies(max);
        }
        X_Log.debug(ConcurrentEnvironment.class, "check timeout");
        checkTimeouts(max);
      }
    }while(! isEmpty());
    }catch (TimeoutException e) {
      return false;
    }finally {
      if (X_Debug.isBenchmark()) {
        X_Log.debug("Spent "+(now()-(max-timeout))+" millis flushing environment");
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

  public void shutdown() {
    stop = true;
  }

  public boolean isStopped() {
    if (stop) {
      return true;
    }
    for (Thread thread : getThreads()) {
      if (thread == Thread.currentThread()) {
        continue;
      }
      if (thread.isAlive()) {
        touched = X_Process.now();
        return false;
      }
    }
    for (Do ignored : getDeferred()) {
      touched = X_Process.now();
      return false;
    }
    for (Do ignored : getFinally()) {
      touched = X_Process.now();
      return false;
    }

    return X_Process.now() - touched > TTL;
  }
}
