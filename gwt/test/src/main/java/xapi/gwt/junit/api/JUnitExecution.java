package xapi.gwt.junit.api;

import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.util.X_Util;
import xapi.util.api.ProvidesValue;
import xapi.util.api.ReceivesValue;

import java.util.function.BooleanSupplier;

/**
 * Created by james on 18/10/15.
 */
public class JUnitExecution<Context> {
  private int[] timeouts;
  private int[] intervals;

  private int maxTimeoutsLeft;
  private int maxIntervalsLeft;

  private BooleanSupplier[] beforeFinished;
  private ReceivesValue<Throwable>[] onFinished;

  private Throwable error;
  private Double deadline;

  private Context context;

  public JUnitExecution() {
    clear();
  }

  public boolean isFinished() {
    return allDone(beforeFinished) || isErrorState();
  }

  public boolean isTimeoutsClear() {
    return (timeouts.length <= maxTimeoutsLeft) && (intervals.length <= maxIntervalsLeft);
  }

  protected boolean allDone(BooleanSupplier[] beforeFinished) {
    for (BooleanSupplier before : beforeFinished) {
      if (!before.getAsBoolean()) {
        return false;
      }
    }
    return true;
  }

  public JUnitExecution<Context> onBeforeFinished(BooleanSupplier test) {
    beforeFinished = X_Util.pushOnto(beforeFinished, test);
    return this;
  }


  public JUnitExecution<Context> onFinished(ReceivesValue<Throwable> callback) {
    onFinished = X_Util.pushOnto(onFinished, callback);
    return this;
  }

  public void autoClean() {
    onFinished(e->this.clear());
  }

  protected boolean isErrorState() {
    return error != null ||
        (deadline != null && X_Time.isPast(deadline));
  }

  public JUnitExecution<Context> setDeadline(double deadline) {
    this.deadline = deadline;
    return this;
  }

  public JUnitExecution<Context> clearDeadline() {
    this.deadline = null;
    return this;
  }

  /**
   * Completely destroys this execution and reseting it to a clean state.
   * <p>
   * If you want your context object to survive, stash it in between invocations.
   */
  public JUnitExecution<Context> clear() {
    clearDeadline();
    error = null;
    context = null;
    kill(timeouts, intervals);
    timeouts = new int[0];
    intervals = new int[0];
    beforeFinished = new BooleanSupplier[]{
        // The default test is that the timeouts and intervals are cleared.
        this::isTimeoutsClear
    };
    return this;
  }

  private native void kill(int[] timeouts, int[] intervals)
  /*-{
      timeouts.forEach(clearTimeout);
      intervals.forEach(clearInterval);
  }-*/;

  /**
   * Report an error of any kind. Does nothing if the Throwable parameter is null.
   * <p>
   * If the string message is expensive to construct,
   * and you are too lazy to do the null check yourself,
   * use {@link JUnitExecution#reportError(Throwable, ProvidesValue)},
   * so you can instead send a lambda, and let the null check prevent potentially
   * wasteful .toString() invocations.
   */
  public void reportError(Throwable error, String message) {
    if (error != null) {
      // Never overwrite an error if somebody sends null
      this.error = error;
      try{
        X_Log.error(getClass(), "\n", message, "\n", error);
      } catch (Error rethrow){
        throw rethrow;
      } catch (Throwable ignored){
      }
    }
  }


  /**
   * Report an error of any kind. Does nothing if the Throwable parameter is null.
   * <p>
   * Prefer this method if the debugging string is relatively expensive to construct.
   * <p>
   * This method is final so you only have to override the method which accepts a string.
   */
  public final void reportError(Throwable error, ProvidesValue<String> message) {
    if (error != null) {
      reportError(error, message.get());
    }
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public void finish() {
    final ReceivesValue<Throwable>[] copy = onFinished;
    onFinished = new ReceivesValue[0];
    for (ReceivesValue<Throwable> callback : copy) {
      callback.set(error);
    }
  }

  public void normalizeLimits() {
    maxIntervalsLeft = intervals.length;
    maxTimeoutsLeft = timeouts.length;
  }
}
