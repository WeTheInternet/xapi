package xapi.process;

import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.process.api.AsyncLock;
import xapi.process.api.Process;
import xapi.process.api.ProcessController;
import xapi.process.service.ConcurrencyService;

import javax.inject.Provider;
import java.util.concurrent.Future;

public class X_Process {

  private static final Provider<ConcurrencyService> service = X_Inject
      .singletonLazy(ConcurrencyService.class);

  public static <T> void resolve(Future<T> future, In1<T> receiver) {
    service.get().resolve(future, receiver);
  }

  public static <T> void blockInBackground(Future<T> future, In1<T> receiver) {
    runDeferred(()->
      service.get().resolve(future, receiver)
    );
  }

  public static void runDeferred(Do cmd) {
    service.get().runDeferred(cmd);
  }

  public static void runDeferredUnsafe(DoUnsafe cmd) {
    service.get().runDeferred(cmd);
  }

  public static void runEventually(Do cmd) {
    service.get().runEventually(cmd);
  }

  public static void runFinallyUnsafe(DoUnsafe cmd) {
    runFinally(cmd);
  }
  public static void runFinally(Do cmd) {
    service.get().runFinally(cmd);
  }

  public static <T> void runFinally(In1<T> cmd, Out1<T> from) {
    service.get().runFinally(cmd.provide(from));
  }

  public static void runTimeout(Do cmd, int milliDelay) {
    service.get().runTimeout(cmd, milliDelay);
  }

  public static Thread newThread(Do cmd) {
    return service.get().newThread(cmd);
  }

  public static <T> ProcessController<T> newProcess(Process<T> process) {
    return service.get().newProcess(process);
  }

  public static boolean flush(int timeout) {
    return flush(Thread.currentThread(), timeout);
  }

  public static boolean flush(Thread thread, int timeout) {
    return service.get().flush(thread, timeout);
  }

  public static boolean trySleep(int millis) {
    return service.get().trySleep(millis);
  }

  public static void kill(Thread thread, int timeout) {
    service.get().kill(thread, timeout);
  }

  public static double threadStartTime() {
    return service.get().threadStartTime(Thread.currentThread());
  }

  public static double now() {
    return service.get().now();
  }

  public static AsyncLock newLock() {
    return service.get().newLock();
  }

  public static <T> void runWhenReadyUnsafe(Lazy<T> io, In1Unsafe<T> callback) {
    runWhenReady(io, callback);
  }
  public static <T> void runWhenReady(Lazy<T> io, In1<T> callback) {
    if (io.isFull1()) {
      callback.in(io.out1());
    } else {
      runDeferred(()->{
        runWhenReady(io, callback);
      });
    }
  }
}
