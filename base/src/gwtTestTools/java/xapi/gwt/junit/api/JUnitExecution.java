package xapi.gwt.junit.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.util.X_Util;
import xapi.util.api.ProvidesValue;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Created by james on 18/10/15.
 */
public class JUnitExecution<Context> {
  private int[] timeouts;
  private int[] intervals;

  private int maxTimeoutsLeft;
  private int maxIntervalsLeft;

  private BooleanSupplier[] beforeFinished;
  private BiFunction<Method, Throwable, Callable<Boolean>>[] onFinishedMethod;
  private Function<Method, Callable<Boolean>>[] onStartMethod;
  private BiFunction<Class<?>, Object, Callable<Boolean>>[] onStartClass;
  private Function<Map<Method, Throwable>, Callable<Boolean>>[] onFinishedClass;

  private Throwable error;
  private Double deadline;

  private Context context;
  private Object instance;

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

  public JUnitExecution<Context> onFinished(BiFunction<Method, Throwable, Callable<Boolean>> callback) {
    onFinishedMethod = X_Util.pushOnto(onFinishedMethod, callback);
    return this;
  }

  public JUnitExecution<Context> onStartMethod(Function<Method, Callable<Boolean>> callback) {
    onStartMethod = X_Util.pushOnto(onStartMethod, callback);
    return this;
  }

  public JUnitExecution<Context> onStartClass(BiFunction<Class<?>, Object, Callable<Boolean>> callback) {
    onStartClass = X_Util.pushOnto(onStartClass, callback);
    return this;
  }

  public JUnitExecution<Context> onFinishedClass(Function<Map<Method,Throwable>, Callable<Boolean>> callback) {
    onFinishedClass = X_Util.pushOnto(onFinishedClass, callback);
    return this;
  }

  public void autoClean() {
    onFinishedClass(e -> {
          this.clear();
          return null;
    });
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
    if (timeouts != null) {
      kill(timeouts, intervals);
    }
    timeouts = new int[0];
    intervals = new int[0];
    onStartClass = new BiFunction[0];
    onStartMethod = new Function[0];
    onFinishedMethod = new BiFunction[0];
    onFinishedClass = new Function[0];

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

  public Iterable<Callable<Boolean>> finishClass(Map<Method, Throwable> results) {
    final Function<Map<Method, Throwable>, Callable<Boolean>>[] copy = onFinishedClass;
    onFinishedClass = new Function[0];
    IntTo<Callable<Boolean>> delays = X_Collect.newList(Callable.class);
    for (Function<Map<Method, Throwable>, Callable<Boolean>> callback : copy) {
      final Callable<Boolean> delay = callback.apply(results);
      if (delay != null) {
        delays.add(delay);
      }
    }
    return delays.forEach();
  }

  public Iterable<Callable<Boolean>> finishMethod(Method m, Throwable e) {
    IntTo<Callable<Boolean>> delays = X_Collect.newList(Callable.class);
    for (BiFunction<Method, Throwable, Callable<Boolean>> callback : onFinishedMethod) {
      final Callable<Boolean> delay = callback.apply(m, e);
      if (delay != null) {
        delays.add(delay);
      }
    }
    if (delays.isEmpty()) {
      error = null;
    } else {
      delays.add(()->{
        error = null;
        return true;
      });
    }
    return delays.forEach();
  }

  public void normalizeLimits() {
    maxIntervalsLeft = intervals.length;
    maxTimeoutsLeft = timeouts.length;
  }

  public Iterable<Callable<Boolean>> startMethod(Method method) {
    IntTo<Callable<Boolean>> delays = X_Collect.newList(Callable.class);
    for (Function<Method, Callable<Boolean>> callback : onStartMethod) {
      final Callable<Boolean> delay = callback.apply(method);
      if (delay != null) {
        delays.add(delay);
      }
    }
    return delays.forEach();
  }

  public Iterable<Callable<Boolean>> startClass(Class<?> testClass, Object inst) {
    IntTo<Callable<Boolean>> delays = X_Collect.newList(Callable.class);
    for (BiFunction<Class<?>, Object, Callable<Boolean>> callback : onStartClass) {
      final Callable<Boolean> delay = callback.apply(testClass, inst);
        if (delay != null) {
          delays.add(delay);
        }
      }
    return delays.forEach();
  }

  public Object getInstance() {
    return instance;
  }

  public void setInstance(Object instance) {
    this.instance = instance;
  }

  public boolean hasError() {
    return error != null;
  }

  public Throwable getError() {
    return error;
  }
}
