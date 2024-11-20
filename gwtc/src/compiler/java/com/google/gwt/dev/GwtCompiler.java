package com.google.gwt.dev;

import xapi.time.X_Time;

import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;

import java.util.concurrent.FutureTask;

public class GwtCompiler {

  public static void main(String[] args) {
    Memory.initialize();
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: "
          + System.getProperty("gwt.jjs.dumpAst"));
    }

    SpeedTracerLogger.init();

    new Thread() {
      {
        setDaemon(true);
      }

      public void run() {
        while (true) {
          System.out.print("");
          System.out.flush();
          System.err.print("");
          System.err.flush();
          X_Time.trySleep(500, 0);
        }
      }

      ;
    }.start();

    if (doCompile(args)) {
      // Exit w/ success code.
      System.out.flush();
      System.exit(0);
    }

    // Exit w/ non-success code.
    System.out.println("Gwt compile failure");
    System.out.flush();
    System.exit(1);
    }

  public static boolean doCompile(String[] args) {
        /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilerOptions options = new CompilerOptionsImpl();
    if (!new ArgProcessor(options).processArgs(args)) {
      return false;
    }
    CompileTask task = logger -> {
        FutureTask<UpdateResult> updater = null;
        if (!options.isUpdateCheckDisabled()) {
          updater = CheckForUpdates.checkForUpdatesInBackgroundThread(
              logger,
              CheckForUpdates.ONE_DAY
          );
        }
        boolean success = new Compiler().compile(logger, options);
        if (success) {
          CheckForUpdates.logUpdateAvailable(logger, updater);
        }
        return success;
    };
    return CompileTaskRunner.runWithAppropriateLogger(options, task);
  }
}
