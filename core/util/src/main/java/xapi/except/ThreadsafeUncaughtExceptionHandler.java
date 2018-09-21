package xapi.except;

import xapi.log.X_Log;
import xapi.util.X_Util;

import java.lang.Thread.UncaughtExceptionHandler;

public class ThreadsafeUncaughtExceptionHandler implements UncaughtExceptionHandler {

  private final Thread launcherThread;

  public ThreadsafeUncaughtExceptionHandler(Thread launcherThread) {
    this.launcherThread = launcherThread;
  }

  public ThreadsafeUncaughtExceptionHandler() {
    this(Thread.currentThread());
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    try {
      X_Log.error(ThreadsafeUncaughtExceptionHandler.class, t, "threw uncaught exception", e);
    } catch (Throwable x){
      e.printStackTrace();
      if (X_Util.unwrap(x) instanceof InterruptedException) {
        Thread launchedThread = Thread.currentThread();
        launchedThread.interrupt();
        if (launchedThread != launcherThread) {
          // Propagate interruption
          X_Log.error(getClass(), "was interrupted; interrupting ",launcherThread);
          launcherThread.interrupt();
        }

      }
    }

  }

}
