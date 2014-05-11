package xapi.time.impl;

import static xapi.time.X_Time.threadStart;

import xapi.time.api.Moment;
import xapi.util.api.ReceivesValue;

public class RunOnce {

  public static Runnable runOnce(final Runnable job) {
    return runOnce(job, false);
  }

  public static Runnable runOnce(final Runnable job, final boolean oncePerMoment) {
    return new Runnable() {
      RunOnce lock = new RunOnce();
      @Override
      public void run() {
        if (lock.shouldRun(oncePerMoment)) {
          job.run();
        }
      }
    };
  }

  public static <X> ReceivesValue<X> setOnce(final ReceivesValue<X> job) {
    return setOnce(job, false);
  }

  public static <X> ReceivesValue<X> setOnce(final ReceivesValue<X> job, final boolean oncePerMoment) {
    return new ReceivesValue<X>() {
      RunOnce lock = new RunOnce();
      @Override
      public void set(X from) {
        if (lock.shouldRun(oncePerMoment)) {
          job.set(from);
        }
      }
    };
  }

  private Moment once;

  public boolean shouldRun(boolean oncePerMoment) {
    if (once == null) {
      synchronized (this) {
        if (once != null) {
          return false;
        }
        once = threadStart();
        return true;
      }
    } else {
      if (oncePerMoment) {
        //do not run more than once per tick of X_Time.
        //this allows you to call this method as many times as you want,
        //but only perform the heavyweight operation of synchronization once.
        Moment now = threadStart();
        synchronized (this) {
          if (once.equals(now)){
            return false;
          }
          once = now;
        }
        return true;
      } else
        return false;
    }
  }

}
