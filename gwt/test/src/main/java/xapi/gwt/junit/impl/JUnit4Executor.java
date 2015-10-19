package xapi.gwt.junit.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.gwt.junit.api.JUnitExecution;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class JUnit4Executor {

  private static JUnitExecution execution;

  private ClassTo<Lifecycle> lifecycles;

  protected JUnit4Executor() {
    if (execution == null) {
      execution = initializeSystem();
    }
    lifecycles = X_Collect.newClassMap(Lifecycle.class);
  }

  public static Method[] findTests(final Class<?> testClass) throws Throwable {
    return new JUnit4Executor().findAnnotated(testClass);
  }

  public static void runTest(final Object inst, final Method m, ReceivesValue<Throwable> callback) throws Throwable {
    final JUnit4Executor exe = new JUnit4Executor();
    exe.run(inst, m, callback);

  }

  public static void runTests(final Class<?> testClass, ReceivesValue<Map<Method, Throwable>> callback) throws Throwable {
    new JUnit4Executor().runAll(testClass, callback);
  }

  private native JUnitExecution initializeSystem()
  /*-{
      var originals = [setTimeout, setInterval, clearTimeout, clearInterval];
      setTimeout = function () {
          var pid = originals[0].apply(this, arguments),
              exe = @JUnit4Executor::execution && @JUnit4Executor::execution.@JUnitExecution::timeouts;
          exe && ( exe[exe.length] = pid );
          return pid;
      };
      setInterval = function () {
          var pid = originals[1].apply(this, arguments),
              exe = @JUnit4Executor::execution && @JUnit4Executor::execution.@JUnitExecution::intervals;
          exe && ( exe[exe.length] = pid );
          return pid;
      };
      clearTimeout = function (pid) {
          originals[2].apply(this, arguments);
          var exe = @JUnit4Executor::execution && @JUnit4Executor::execution.@JUnitExecution::timeouts,
              ind = exe ? exe.indexOf(pid) : -1;
          ind !== -1 && exe.splice(ind, 1);
      };
      clearInterval = function (pid) {
          originals[3].apply(this, arguments);
          var exe = @JUnit4Executor::execution && @JUnit4Executor::execution.@JUnitExecution::intervals,
              ind = exe ? exe.indexOf(pid) : -1;
          ind !== -1 && exe.splice(ind, 1);
      };

      return @JUnitExecution::new()();
  }-*/;

  private Method[] findAnnotated(final Class<?> testClass) {
    final Lifecycle lifecycle = new Lifecycle(testClass);
    return lifecycle.tests.values().toArray(new Method[0]);
  }

  protected void assertPublicZeroArgInstanceMethod(final Method method, final Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    if (Modifier.isStatic(method.getModifiers())) {
      throw new AssertionError("@" + type.getSimpleName() + " methods must not be static");
    }
  }

  protected void assertPublicZeroArgMethod(final Method method, final Class<?> type) {
    if (!Modifier.isPublic(method.getModifiers())) {
      throw new AssertionError("@" + type.getSimpleName() + " methods must be public");
    }
    if (0 != method.getParameterTypes().length) {
      throw new AssertionError("@" + type.getSimpleName() + " methods must be zero-arg");
    }
  }

  protected void assertPublicZeroArgStaticMethod(final Method method, final Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new AssertionError("@" + type.getSimpleName() + " methods must be static");
    }
  }

  protected void debug(final String string, Throwable e) {
    if (GWT.isProdMode()) {
      GWT.log(string + " (" + e + ")");
    } else {
      System.out.println(string);
    }
    while (e != null) {
      e.printStackTrace(System.err);
      e = e.getCause();
    }
  }

  protected ReceivesValue<ReceivesValue<Map<Method, Throwable>>> prepareExecution(
      final Object inst, Lifecycle lifecycle
  ) {
    return (callback) -> {

      final Map<Method, Throwable> result = newMap();

      JUnitExecution controls = prepareToExecute(inst, lifecycle.tests);

      IntTo<Callable<Boolean>> tasks = X_Collect.newList(Class.class.cast(Callable.class));
      tasks.add(lifecycle.beforeClassInvoker(controls));

      for (final Entry<String, Method> test : lifecycle.tests.entrySet()) {
        final Method method = test.getValue();
        Test t = method.getAnnotation(Test.class);
        tasks.add(
            () -> {
              controls.normalizeLimits();
              final double timeout = t == null || t.timeout() == 0 ? getDefaultTimeout() : t.timeout();
              final double deadline = X_Time.nowPlus(timeout);
              final UncaughtExceptionHandler oldHandler = GWT.getUncaughtExceptionHandler();
              final JUnitExecution oldExecution = execution;
              tasks.insert(
                  0, () -> {
                    try {
                      controls.finish();
                    } finally {
                      GWT.setUncaughtExceptionHandler(oldHandler);
                      if (execution == controls) {
                        execution = oldExecution;
                      }
                    }
                    return true;
                  }
              );

              tasks.insert(0, lifecycle.afterInvoker(controls));
              tasks.insert(
                  0, () -> {

                    final Throwable error = execute(t, inst, method);
                    result.put(method, error);
                    if (controls.isTimeoutsClear()) {
                      return true;
                    }

                    final Callable<Boolean>[] runner = new Callable[1];
                    runner[0] = () -> {
                      if (controls.isFinished()) {
                        return true;
                      }
                      if (X_Time.isPast(deadline)) {
                        throw new TimeoutException("Test for " + method + " exceeded timeout of " + timeout + "ms.");
                      }
                      tasks.insert(0, runner[0]);
                      return false;
                    };
                    tasks.insert(0, runner[0]);

                    return false;
                  }
              );
              tasks.insert(0, lifecycle.beforeInvoker(controls));
              // This one will be run first.  It hijacks the uncaught exception handler, and sets the execution context
              tasks.insert(
                  0, () -> {
                    GWT.setUncaughtExceptionHandler(
                        e -> {
                          controls.reportError(e, "Uncaught exception");
                          if (oldHandler != null) {
                            oldHandler.onUncaughtException(e);
                          }
                        }
                    );
                    execution = controls;
                    return true;
                  }
              );

              return true;
            }
        );
      }

      tasks.add(lifecycle.afterClassInvoker(controls));

      RepeatingCommand command = () -> {

        while (!tasks.isEmpty()) {
          final Callable<Boolean> task = tasks.get(0);
          tasks.remove(0);
          boolean more;
          try {
            more = task.call();
          } catch (Throwable e) {
            controls.reportError(e, "Unknown exception in " + task + " for " + inst);
            more = false;
          }
          if (!more) {

            return true;
          }
        }
        return false;
      };

      while (command.execute()) {
        if (controls.isFinished()) {
          // We got lucky.  Nothing deferred occurred.
          finishExecution(controls, result, callback, inst, lifecycle.tests);
        } else {
          // There is a timer.
          Runnable[] wait = new Runnable[1];
          wait[0] = () -> {
            int max = 10000;
            while (command.execute() && max-- > 0)
              ;
            assert max > 0 : "Infinite loop detected in junit execution of " + lifecycle + " on " + inst + ".";
            if (controls.isFinished()) {
              wait[0] = null;
              finishExecution(controls, result, callback, inst, lifecycle.tests);
            } else {
              X_Time.runLater(wait[0]);
            }
          };
          X_Time.runLater(wait[0]);
        }
      }
    };
  }

  protected long getDefaultTimeout() {
    return 30000;
  }
//
//  protected void execute(
//      final Object inst,
//      final Map<String, Method> tests,
//      final List<Method> beforeClass,
//      final List<Method> before,
//      final List<Method> after,
//      final List<Method> afterClass,
//      ReceivesValue<Map<String, Throwable>> callback
//  )
//  throws TestsFailed, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodError {
//    final Map<Method, Throwable> result = new LinkedHashMap<Method, Throwable>();
//    JUnitExecution controls = prepareToExecute(inst, tests);
//
//    try {
//
//      for (final Method m : beforeClass) {
//        m.invoke(null);
//      }
//
//      for (final Entry<String, Method> test : tests.entrySet()) {
//        result.put(test.getValue(), runTest(inst, test.getValue(), before, after));
//      }
//
//    } finally {
//
//      for (final Method m : afterClass) {
//        m.invoke(null);
//      }
//      finishExecution(controls, result, callback, inst, tests);
//    }
//
//    for (final Entry<Method, Throwable> e : result.entrySet()) {
//      if (e.getValue() != null) {
//        final TestsFailed failure = new TestsFailed(result);
//        debug("Tests Failed;\n", failure);
//        throw new AssertionError(failure.toString());
//      }
//    }
//  }

  protected void finishExecution(
      JUnitExecution controls,
      Map<Method, Throwable> result,
      ReceivesValue<Map<Method, Throwable>> callback,
      Object inst, Map<String, Method> tests
  ) {
    controls.finish();
    callback.set(result);
  }

  protected JUnitExecution prepareToExecute(Object inst, Map<String, Method> tests) {
    final JUnitExecution oldExecution = execution;
    final JUnitExecution newExecution = execution = newExecution();
    execution.onFinished(
        e -> {
          if (execution == newExecution) {
            execution = oldExecution;
          }
        }
    );
    return execution;
  }

  protected JUnitExecution newExecution() {
    return new JUnitExecution();
  }

  protected List<Method> newList(final Map<String, Method> beforeClass, final boolean reverse) {
    List<Method> list;
    if (reverse) {
      list = new LinkedList<Method>();
      for (final Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(0, e.getValue());
      }
    } else {
      list = new ArrayList<Method>();
      for (final Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(e.getValue());
      }
    }
    return list;
  }

  protected <K, V> Map<K, V> newMap() {
    return new LinkedHashMap<>();
  }

  protected void run(final Object inst, final Method m, ReceivesValue<Throwable> callback) throws Throwable {
    final Lifecycle lifecycle = newLifecycle(m.getDeclaringClass())
        .withOnlyOneMethod(m);
    final ReceivesValue<ReceivesValue<Map<Method, Throwable>>> exe = prepareExecution(inst, lifecycle);
    final ReceivesValue<Map<Method, Throwable>> delegate =
        map -> callback.set(map.values().iterator().next());
    scheduleExecution(exe, delegate);
    exe.set(delegate);
  }

  private void scheduleExecution(
      ReceivesValue<ReceivesValue<Map<Method, Throwable>>> exe,
      ReceivesValue<Map<Method, Throwable>> delegate
  ) {
    exe.set(delegate);
  }

  private Lifecycle newLifecycle(Class<?> cls) {
    Lifecycle lifecycle = lifecycles.getOrCompute(
        cls,
        c -> initializeLifecycle(c, new Lifecycle(c))
    );
    return lifecycle;
  }

  protected Lifecycle initializeLifecycle(Class<?> cls, Lifecycle lifecycle) {
    return lifecycle;
  }

  protected void runAll(final Class<?> testClass, ReceivesValue<Map<Method, Throwable>> callback) throws Throwable {
    final Lifecycle lifecycle = newLifecycle(testClass);
    if (lifecycle.tests.size() > 0) {
      final Object inst = testClass.newInstance();
      final ReceivesValue<ReceivesValue<Map<Method, Throwable>>> exe = prepareExecution(inst, lifecycle);
      exe.set(callback);
    }
  }

  protected Throwable runTest(
      final Object inst,
      final Method value,
      final List<Method> before,
      final List<Method> after
  ) {

    Test test = null;
    try {
      test = value.getAnnotation(Test.class);
    } catch (final Exception e) {
      debug("Error getting @Test annotation", e);
    }

    try {

      debug("Executing " + value + " on " + inst, null);
      for (final Method m : before) {
        try {
          m.invoke(inst);
        } catch (Throwable e) {
          X_Debug.rethrow(e);
        }
      }
      return execute(test, inst, value);
    } finally {
      for (final Method m : after) {
        try {
          m.invoke(inst);
        } catch (final Throwable e) {
          debug("Error calling after methods", e);
          return e;
        }
      }
    }
  }

  protected Throwable execute(Test test, Object inst, Method value) {
    final Class<? extends Throwable> expected = test == null ? Test.None.class : test.expected();
    // We'll have to figure out timeouts in the actual JUnit jvm
    try {
      try {
        value.invoke(inst);
      } catch (final InvocationTargetException e) {
        return e.getCause();
      }

      if (expected != Test.None.class) {
        return new AssertionError(
            "Method " + value + " was supposed to throw " + expected.getName()
                + ", but failed to do so"
        );
      }
      return null;
    } catch (final Throwable e) {
      // Allow user to `throw null;` if they want to "short circuit to success".
      // TODO make this only work as an opt in...
      if (e == null) {
        return null;
      }
      return expected.isAssignableFrom(e.getClass()) ? null : e;
    }
  }

  protected class Lifecycle {
    protected Map<String, Method> after = newMap();
    protected Map<String, Method> afterClass = newMap();
    protected Map<String, Method> before = newMap();
    protected Map<String, Method> beforeClass = newMap();
    protected Map<String, Method> tests = newMap();

    public Lifecycle(Lifecycle from) {
      beforeClass.putAll(from.beforeClass);
      before.putAll(from.before);
      tests.putAll(from.tests);
      after.putAll(from.after);
      afterClass.putAll(from.afterClass);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public Lifecycle(final Class testClass) {
      Class init = testClass;
      while (init != null && init != Object.class) {
        try {
          for (final Method method : init.getMethods()) {
            if (method.getAnnotation(Test.class) != null) {
              assertPublicZeroArgInstanceMethod(method, Test.class);
              if (!tests.containsKey(method.getName())) {
                tests.put(method.getName(), method);
              }
              final Method previous = tests.put(method.getName(), method);
              if (previous != null) {
                kill(previous);
              }
            }
            addLifecycleMethods(method);
          }
        } catch (final NoSuchMethodError ignored) {
          debug("Class " + init + " is not enhanced", null);
        } catch (final Exception e) {
          debug("Error getting declared methods for " + init, e);
        }
        init = init.getSuperclass();
      }
    }

    protected void kill(Method previous) {

    }

    public List<Method> after() {
      return newList(after, true);
    }

    public List<Method> afterClass() {
      return newList(afterClass, true);
    }

    public List<Method> before() {
      return newList(before, true);
    }

    public List<Method> beforeClass() {
      return newList(beforeClass, true);
    }

    private void addLifecycleMethods(final Method method) {
      if (method.getAnnotation(Before.class) != null) {
        assertPublicZeroArgInstanceMethod(method, Before.class);
        before.putIfAbsent(method.getName(), method);
      }
      if (method.getAnnotation(BeforeClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, BeforeClass.class);
        beforeClass.putIfAbsent(method.getName(), method);
      }
      if (method.getAnnotation(After.class) != null) {
        assertPublicZeroArgInstanceMethod(method, After.class);
        after.putIfAbsent(method.getName(), method);
      }
      if (method.getAnnotation(AfterClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, AfterClass.class);
        afterClass.putIfAbsent(method.getName(), method);
      }
    }

    public Lifecycle withOnlyOneMethod(Method m) {
      Lifecycle copy = new Lifecycle(this);
      copy.tests.clear();
      copy.tests.put(m.getName(), m);
      return copy;
    }

    public Callable<Boolean> invoker(JUnitExecution controls, Collection<Method> list) {
      return () -> {
        for (final Method m : list) {
          m.invoke(null);
        }
        return controls.isTimeoutsClear();
      };
    }

    public Callable<Boolean> beforeClassInvoker(JUnitExecution controls) {
      return invoker(controls, beforeClass());
    }

    public Callable<Boolean> beforeInvoker(JUnitExecution controls) {
      return invoker(controls, before());
    }

    public Callable<Boolean> afterClassInvoker(JUnitExecution controls) {
      return invoker(controls, afterClass());
    }

    public Callable<Boolean> afterInvoker(JUnitExecution controls) {
      return invoker(controls, after());
    }
  }

}
