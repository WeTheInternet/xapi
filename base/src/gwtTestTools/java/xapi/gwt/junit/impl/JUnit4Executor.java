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
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.debug.X_Debug;
import xapi.util.X_Runtime;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.reflect.shared.GwtReflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class JUnit4Executor {

  public static final Throwable SUCCESS = new Throwable();
  protected static JUnitExecution execution;

  private ClassTo<Lifecycle> lifecycles;

  protected JUnit4Executor() {
    if (execution == null) {
      execution = X_Runtime.isGwt() ? initializeSystem() : new JUnitExecution();
    }
    lifecycles = X_Collect.newClassMap(Lifecycle.class);
  }

  public static Method[] findTests(final Class<?> testClass) throws Throwable {
    return new JUnit4Executor().findAnnotated(testClass);
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
      controls.setInstance(inst);
      setExecution(controls, inst);

      IntTo<Callable<Boolean>> tasks = X_Collect.newList(Callable.class);

      tasks.add(()->{
            final Iterable<Callable<Boolean>> delays = controls.startClass(lifecycle.getTestClass(), inst);
            delays.forEach(delay->tasks.insert(0, delay));
            return true;
      });
      tasks.add(lifecycle.beforeClassInvoker(controls));

      for (final Entry<String, Method> test : lifecycle.tests.entrySet()) {
        final Method method = test.getValue();
        Test t = method.getAnnotation(Test.class);
        tasks.add(
            () -> {
              controls.normalizeLimits();
              final Iterable<Callable<Boolean>> startDelays = controls.startMethod(method);
              final double timeout = t == null || t.timeout() == 0 ? getDefaultTimeout() : t.timeout();
              final double deadline = X_Time.nowPlus(timeout);
              final UncaughtExceptionHandler oldHandler = GWT.getUncaughtExceptionHandler();
              final JUnitExecution oldExecution = execution;
              // We insert the tasks backwards, from 0
              tasks.insert(
                  0, () -> {
                    try {
                      if (controls.hasError()) {
                        result.put(method, controls.getError());
                      }
                      final Iterable<Callable<Boolean>> delays = controls.finishMethod(method, result.get(method));
                      delays.forEach(delay->tasks.insert(0, delay));
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
              startDelays.forEach(delay->tasks.insert(0, delay));
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

      tasks.add(()->{
            int was = tasks.size();
            final Iterable<Callable<Boolean>> delays = controls.finishClass(result);
            delays.forEach(delay->tasks.insert(0, delay));
            return was != tasks.size();
          });

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

      while (command.execute())
        ;
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
    };
  }

  protected long getDefaultTimeout() {
    return 30000;
  }

  protected void finishExecution(
      JUnitExecution controls,
      Map<Method, Throwable> result,
      ReceivesValue<Map<Method, Throwable>> callback,
      Object inst, Map<String, Method> tests
  ) {
    callback.set(result);
  }

  protected JUnitExecution prepareToExecute(Object inst, Map<String, Method> tests) {
    final JUnitExecution oldExecution = execution;
    final JUnitExecution newExecution = execution = newExecution();
    execution.onFinishedClass(
        e -> {
          if (execution == newExecution) {
            execution = oldExecution;
            setExecution(oldExecution, inst);
          }
          return null;
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

  public void run(final Object inst, final Method m, ReceivesValue<Throwable> callback) throws Throwable {
    final Lifecycle lifecycle = newLifecycle(m.getDeclaringClass())
        .withOnlyOneMethod(m);
    final ReceivesValue<ReceivesValue<Map<Method, Throwable>>> exe = prepareExecution(inst, lifecycle);
    final ReceivesValue<Map<Method, Throwable>> delegate =
        map -> callback.set(map.values().iterator().next());
    scheduleExecution(exe, delegate);
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

  public void runAll(final Class<?> testClass, Object inst, ReceivesValue<Map<Method, Throwable>> callback) {
    final Lifecycle lifecycle = newLifecycle(testClass);
    if (lifecycle.tests.size() > 0) {
      final ReceivesValue<ReceivesValue<Map<Method, Throwable>>> exe = prepareExecution(inst, lifecycle);
      exe.set(callback);
    }
  }

  protected Throwable execute(Test test, Object inst, Method value) {
    final Class<? extends Throwable> expected = test == null ? Test.None.class : test.expected();
    // We'll have to figure out timeouts in the actual JUnit jvm
    try {
      try {
        value.invoke(inst);
      } catch (final InvocationTargetException e) {
        throw e.getCause();
      }

      if (expected != Test.None.class) {
        return new AssertionError(
            "Method " + value + " was supposed to throw " + expected.getName()
                + ", but failed to do so"
        );
      }
      return SUCCESS;
    } catch (final Throwable e) {
      // Allow user to `throw null;` if they want to "short circuit to success".
      // TODO make this only work as an opt in...
      if (e == null) {
        return SUCCESS;
      }
      if (!expected.isAssignableFrom(e.getClass())) {
        X_Debug.rethrow(e);
      }
      return expected.isAssignableFrom(e.getClass()) ? SUCCESS : e;
    }
  }

  protected void findAndSetField(Predicate<Class> matcher, Object value, Object inst, boolean forceSet) {
    try {
      Class<?> declaringClass = inst.getClass();
      Field[] fields = GwtReflect.getPublicFields(declaringClass);
      for (Field field : fields) {
        if (matcher.test(field.getType())) {
          try {
            if (forceSet || field.get(inst) == null) {
              field.set(inst, value);
            }
          } catch (Throwable e) {
            X_Log.warn(
                getClass(),
                "findAndSetField for " + matcher + " on " + declaringClass + " (" + inst + ") encountered an error",
                e
            );
          }
        }
      }
      while (declaringClass != null && declaringClass != Object.class) {
        fields = GwtReflect.getDeclaredFields(declaringClass);
        for (Field field : fields) {
          if (matcher.test(field.getType())) {
            try {
              field.setAccessible(true);
              if (forceSet || field.get(inst) == null) {
                field.set(inst, value);
              }
            } catch (Throwable e) {
              X_Log.warn(getClass(), "findAndSetField for "+matcher+" on "+declaringClass+" ("+inst+") encountered an error", e);
            }
          }
        }
        declaringClass = declaringClass.getSuperclass();
      }

    } catch (Throwable e) {
      X_Log.warn(getClass(), "Unable to set value "+value+" on instance "+inst+" using matcher "+matcher);
    }
  }


  protected void setExecution(JUnitExecution execution, Object inst) {
    execution.autoClean();
    findAndSetField(execution.getClass()::isAssignableFrom, execution, inst, true);
  }

  public static String debug(Object message, Throwable e) {
    final StringBuilder b = new StringBuilder();
    b.append(message);
    b.append('\n');
    b.append("<pre style='color:red;'>");
    while (e != null) {
      b.append(e);
      b.append('\n');
      for (final StackTraceElement trace : e.getStackTrace()) {
        b.append('\t')
            .append(trace.getClassName())
            .append('.')
            .append(trace.getMethodName())
            .append(' ')
            .append(trace.getFileName())
            .append(':')
            .append(trace.getLineNumber())
            .append('\n');
      }
      e = e.getCause();
    }
    b.append("</pre>");
    return b.toString();
  }

  /**
   * Synchronously execute the supplied method on the supplied object, rethrowing any exceptions we encounter.
   *
   * This method is deprecated and discouraged, since any test with asynchronicity must use a callback.
   */
  @Deprecated
  public static void runTest(Object on, Method method) throws Throwable {
    Throwable[] result = new Throwable[0];
    new JUnit4Executor().run(on, method, (s)->{
          if (s != SUCCESS){
            result[0] = s;
          }
    });
    if (result[0] != null) {
      throw result[0];
    }
  }

  public static void runTests(Class<?> cls) throws Exception {
    new JUnit4Executor().runAll(
        cls, cls.newInstance(), results -> {
          final Iterator<Entry<Method, Throwable>> itr = results.entrySet().iterator();
          Throwable last = null;
          while (itr.hasNext()) {
            final Entry<Method,Throwable> next = itr.next();
            if (next.getValue() == SUCCESS || next.getValue() == null) {
              itr.remove();
            } else {
              last = next.getValue();
              X_Log.error("Failure invoking "+next.getKey().getName(), last);
            }
          }
          if (!results.isEmpty()) {
            throw new RuntimeException(results.size()+ " tests in "+cls+" failed: "+results, last);
          }
        }
    );
  }

  protected class Lifecycle {
    protected final Class<?> forClass;
    protected Map<String, Method> after = newMap();
    protected Map<String, Method> afterClass = newMap();
    protected Map<String, Method> before = newMap();
    protected Map<String, Method> beforeClass = newMap();
    protected Map<String, Method> tests = newMap();

    public Lifecycle(Lifecycle from) {
      forClass = from.forClass;
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
      Class initClass = forClass = testClass;

      while (initClass != null && initClass != Object.class) {
        try {
          for (final Method method : initClass.getMethods()) {
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
          debug("Class " + initClass + " is not enhanced", null);
        } catch (final Exception e) {
          debug("Error getting declared methods for " + initClass, e);
        }
        initClass = initClass.getSuperclass();
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
          m.invoke(controls.getInstance());
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

    public Class<?> getTestClass() {
      return forClass;
    }
  }

}
