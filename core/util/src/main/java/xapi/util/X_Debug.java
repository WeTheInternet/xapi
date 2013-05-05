package xapi.util;

import java.util.Arrays;

import xapi.log.X_Log;
import xapi.util.api.ErrorHandler;

public class X_Debug {

	private X_Debug() {}
	
  public static void debug(Throwable e) {
    //TODO: give an injectable service to handle debug messages
    //This would be good for threadlocal processes,
    //so the object inspecting the throwable will have context for the exception
    e.printStackTrace();
  }


  public static boolean isBenchmark() {
    return X_Runtime.isDebug();
  }


  public static void inspect(String qualifiedBinaryName) {
    X_Log.error(qualifiedBinaryName);
    if (X_Runtime.isDebug()) {
      StackTraceElement[] trace = new RuntimeException().getStackTrace();
      X_Log.error(Arrays.asList(trace).toString().replaceAll(", ", "\n  ") );
    }
  }

	@SuppressWarnings("rawtypes")
	public static ErrorHandler defaultHandler() {
		return DefaultHandler.handler;
	}

  public static void maybeRethrow(Throwable e) {
    if (X_Runtime.isDebug())
      debug(e);
    Throwable unwrapped = X_Util.unwrap(e);
    if (
        // NEVER eat these! Getting interrupted while sleeping is no big deal,
        // but if you are running, unless you are doing something mission critical,
        // an interruption needs to bubble up so threads can be shut down.
        unwrapped instanceof InterruptedException
        ) {
      // avoid new wrapper instances;
      // wrap will rethrow Error, or return a RuntimeException
      throw X_Util.rethrow(e);
    }
  }

  public static RuntimeException rethrow(Throwable e) {
    debug(e);
    throw X_Util.rethrow(e);
  }

}
class DefaultHandler implements ErrorHandler<Throwable> {
	static final DefaultHandler handler = new DefaultHandler();
	private DefaultHandler() { }
	@Override
	public void onError(Throwable e) {
		X_Log.error(e);
	}
};