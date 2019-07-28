package xapi.process;

import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.process.api.AsyncLock;
import xapi.process.api.Process;
import xapi.process.api.ProcessController;
import xapi.process.service.ConcurrencyService;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class X_Process {

  private static final Lazy<ConcurrencyService> service = X_Inject
      .singletonLazy(ConcurrencyService.class);

  public static <T> void resolve(Future<T> future, In1<T> receiver) {
    service.out1().resolve(future, receiver);
  }

  public static <T> void blockInBackground(Future<T> future, In1<T> receiver) {
    runDeferred(()->
      service.out1().resolve(future, receiver)
    );
  }

  public static void runDeferred(Do cmd) {
    service.out1().runDeferred(cmd);
  }

  public static void runInClassloader(ClassLoader loader, DoUnsafe cmd) {
    service.out1().runInClassloader(loader, cmd);
  }

  public static void runDeferredUnsafe(DoUnsafe cmd) {
    service.out1().runDeferred(cmd);
  }

  public static void runEventually(Do cmd) {
    service.out1().runEventually(cmd);
  }

  public static void runFinallyUnsafe(DoUnsafe cmd) {
    runFinally(cmd);
  }
  public static void runDoubleFinally(Do cmd) {
    runFinally(()->
      runFinally(cmd)
    );
  }
  public static void runFinally(Do cmd) {
    service.out1().runFinally(cmd);
  }

  public static <T> void runFinally(In1<T> cmd, Out1<T> from) {
    service.out1().runFinally(cmd.provideDeferred(from));
  }

  public static void runTimeout(Do cmd, int milliDelay) {
    service.out1().runTimeout(cmd, milliDelay);
  }

  public static Thread newThread(Do cmd) {
    return service.out1().newThread(cmd);
  }

  public static Thread newThreadUnsafe(DoUnsafe cmd) {
    return service.out1().newThread(cmd);
  }

  public static Thread newThread(Do cmd, ThreadGroup group) {
    return service.out1().newThread(cmd, group);
  }

  public static <T> ProcessController<T> newProcess(Process<T> process) {
    return service.out1().newProcess(process);
  }

  public static boolean flush(int timeout) {
    return flush(Thread.currentThread(), timeout);
  }

  public static boolean flush(Thread thread, int timeout) {
    return service.out1().flush(thread, timeout);
  }

  public static boolean trySleep(int millis) {
    return service.out1().trySleep(millis);
  }

  public static void kill(Thread thread, int timeout) {
    service.out1().kill(thread, timeout);
  }

  public static double threadStartTime() {
    return service.out1().threadStartTime(Thread.currentThread());
  }

  public static double now() {
    return service.out1().now();
  }

  public static AsyncLock newLock() {
    return service.out1().newLock();
  }

  public static <T> void runWhenReadyUnsafe(Lazy<T> io, In1Unsafe<T> callback) {
    runWhenReady(io, callback);
  }

  public static <T> void runWhenReady(Lazy<T> io, In1<T> callback) {
    runWhenReady(io, callback.ignore2());
  }

  public static <T> void runWhenReady(Lazy<T> io, In2<T, Throwable> callback) {
    runWhenReady(io, callback, 0);
  }

  public static <T> void runWhenReady(Lazy<T> io, In2<T, Throwable> callback, int cnt) {
    if (io.isFull1()) {
      boolean calledCallback = false;
      try {
        final T result = io.out1();
        calledCallback = true;
        callback.in(result, null);
      } catch (Throwable t) {
        if (!calledCallback) {
          callback.in(null ,t);
        }
        throw t;
      }
    } else {
      if (cnt < 10) {
        runDeferred(()->
          runWhenReady(io, callback)
        );
      } else {
        runTimeout(()-> runWhenReady(io, callback, cnt+1), cnt * 2);
      }
    }
  }

  public static boolean isInProcess() {
    return service.out1().isInProcess();
  }

  public static Do scheduleInterruption(long blocksFor, TimeUnit unit) {
    return service.out1().scheduleInterruption(blocksFor, unit);
  }

  public static void shutdown() {
    if (service.isResolved()) {
      service.out1().shutdown();
    }
  }
}
