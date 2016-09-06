package xapi.process;

import java.util.concurrent.Future;

import javax.inject.Provider;

import xapi.fu.Do;
import xapi.inject.X_Inject;
import xapi.process.api.AsyncLock;
import xapi.process.api.Process;
import xapi.process.api.ProcessController;
import xapi.process.service.ConcurrencyService;
import xapi.util.api.ReceivesValue;

public class X_Process {

  private static final Provider<ConcurrencyService> service = X_Inject
      .singletonLazy(ConcurrencyService.class);

  public static <T> void block(Future<T> future, ReceivesValue<T> receiver) {
    service.get().resolve(future, receiver);
  }

  public static void runDeferred(Do cmd) {
    service.get().runDeferred(cmd);
  }

  public static void runEventually(Do cmd) {
    service.get().runEventually(cmd);
  }

  public static void runFinally(Do cmd) {
    service.get().runFinally(cmd);
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

}
