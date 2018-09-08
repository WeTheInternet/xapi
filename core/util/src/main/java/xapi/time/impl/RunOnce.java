package xapi.time.impl;

import xapi.fu.Do;
import xapi.time.api.Moment;
import xapi.util.api.ReceivesValue;

import static xapi.time.X_Time.threadStart;

public class RunOnce {

  @SuppressWarnings("rawtypes")
  private static final ReceivesValue NO_OP_RECEIVER = new ReceivesValue.NoOp();

  public static Do runOnce(final Do job) {
    return runOnce(job, false);
  }

  public static Do runOnce(final Do job, final boolean oncePerMoment) {
    if (oncePerMoment) {
      return new Do() {
        RunOnce lock = new RunOnce();
        @Override
        public void done() {
          if (lock.shouldRun(oncePerMoment)) {
            job.done();
          }
        }
      };
    } else {
      return new Do() {
        Do once = job;
        @Override
        public void done() {
          once.done();
          once = Do.NOTHING;
        }
      };
    }
  }

  public static <X> ReceivesValue<X> setOnce(final ReceivesValue<X> job) {
    return setOnce(job, false);
  }

  public static <X> ReceivesValue<X> setOnce(final ReceivesValue<X> job, final boolean oncePerMoment) {
    if (oncePerMoment) {
      return new ReceivesValue<X>() {
        RunOnce lock = new RunOnce();
        @Override
        public void set(X from) {
          if (lock.shouldRun(oncePerMoment)) {
            job.set(from);
          }
        }
      };
    } else {
      return new ReceivesValue<X>() {
        volatile ReceivesValue<X> once = job;
        @Override
        @SuppressWarnings("unchecked")
        public void set(X from) {
          final ReceivesValue<X> task;
          synchronized (this) {
            task = once;
            once = NO_OP_RECEIVER;
          }
          task.set(from);
        }
      };
    }
  }

  private Moment once;

  public boolean hasRun() {
    synchronized (this) {
      return once != null;
    }
  }
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
      } else {
        return false;
      }
    }
  }

}
