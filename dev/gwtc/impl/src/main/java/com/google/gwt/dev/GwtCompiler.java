package com.google.gwt.dev;

import java.util.concurrent.FutureTask;

import xapi.time.X_Time;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompileTaskRunner;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.Compiler.CompilerOptionsImpl;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;

public class GwtCompiler {


  public static void main(String[] args) {
    Memory.initialize();
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: "
          + System.getProperty("gwt.jjs.dumpAst"));
    }

    SpeedTracerLogger.init();

    new Thread(){
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
      };
    }.start();
    
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilerOptions options = new CompilerOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          FutureTask<UpdateResult> updater = null;
          if (!options.isUpdateCheckDisabled()) {
            updater = CheckForUpdates.checkForUpdatesInBackgroundThread(logger,
                CheckForUpdates.ONE_DAY);
          }
          boolean success = new Compiler(options).run(logger);
          if (success) {
            CheckForUpdates.logUpdateAvailable(logger, updater);
          }
          return success;
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        System.out.flush();
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.out.println("Gwt compile failure");
    System.out.flush();
    System.exit(1);
  }
  
}
