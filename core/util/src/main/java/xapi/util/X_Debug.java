package xapi.util;

import xapi.log.X_Log;
import xapi.util.api.ErrorHandler;

import java.io.PrintStream;
import java.util.Arrays;

public class X_Debug {
  private X_Debug() {}

  public static AssertionError recommendAssertions() {
    // Use a single debug String in production, which recommends -ea compiles for far more details
    // in assertion errors.  This can allow you to unit test for AssertionError whenever "something that shouldn't happen" occurs,
    // without sending along large or sensitive debugging strings to production clients.
    return new AssertionError("Recompile with -ea assertions enabled for a better error message.");
  }

  public static class DebugStream extends PrintStream {

    private static final int maxLines = Integer.parseInt(X_Properties.getProperty("xapi.debug.lines", "10"));

    private final PrintStream orig;
    private final int depth;

    public DebugStream(PrintStream orig, int ignoreDepth) {
      super(orig);
      this.depth = ignoreDepth;
      this.orig = orig;
    }

    @Override
    public void println() {
      super.println();
      debugCaller();
    };

    private void debugCaller() {
      RuntimeException e = new RuntimeException();
      e.fillInStackTrace();
      StackTraceElement[] traces = e.getStackTrace();
      int index = depth, end = Math.min(depth+maxLines, traces.length);
      orig.print("\n\t\t @ ");
      for(;index < end; index++ ) {
        orig.print(traces[index]+": ");
      }
      if (X_Runtime.isJava()) {
        flush(orig);
      }
    }

    private void flush(PrintStream orig2) {
      orig.flush();
    }

    public PrintStream append(CharSequence str) {
      // GWT doesn't have an append method, so we hack it here
      print(str.toString());
      return this;
    }

    @Override
    public void print(String s) {
      super.print(s);
      debugCaller();
    }

  }

  public static void debug(Throwable e) {
    // TODO: give an injectable service to handle debug messages
    // This would be good for threadlocal processes,
    // so the object inspecting the throwable will have context for the
    // exception
    while (e != null) {
      e.printStackTrace();
      if (e == e.getCause()) return;
      e = e.getCause();
    }
  }

  public static boolean isBenchmark() {
    return X_Runtime.isDebug();
  }

  public static void inspect(String qualifiedBinaryName) {
    X_Log.error(qualifiedBinaryName);
    if (X_Runtime.isDebug()) {
      StackTraceElement[] trace = new RuntimeException().getStackTrace();
      X_Log.error(Arrays.asList(trace).toString().replaceAll(", ", "\n  "));
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
    unwrapped instanceof InterruptedException) {
      // avoid new wrapper instances;
      // wrap will rethrow Error, or return a RuntimeException
      throw X_Util.rethrow(e);
    }
  }

  public static RuntimeException rethrow(Throwable e) {
    debug(e);
    throw X_Util.rethrow(e);
  }

  public static void traceSystemErr() {
    traceSystemErr(3);
  }
  public static void traceSystemErr(int ignoreDepth) {
    final PrintStream orig = System.err;
    // Avoid cyclic calls, but don't store references...
    if (orig instanceof DebugStream)
      return;
    System.setErr(new DebugStream(orig, ignoreDepth));
    System.err.println("Tracing system.err");
  }

  public static void traceSystemOut() {
    traceSystemOut(3);
  }
  /**
   *
   * @param ignoreDepth
   */
  public static void traceSystemOut(int ignoreDepth) {
    final PrintStream orig = System.out;
    // Avoid cyclic calls, but don't store references...
    if (orig instanceof DebugStream)
      return;
    System.setOut(new DebugStream(orig, ignoreDepth));
    System.out.println("Tracing system.out");
  }
}

class DefaultHandler implements ErrorHandler<Throwable> {
  static final DefaultHandler handler = new DefaultHandler();

  private DefaultHandler() {
  }

  @Override
  public void onError(Throwable e) {
    X_Log.error(e);
  }
};
