package xapi.shell.impl;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.file.X_File;
import xapi.inject.X_Inject;
import xapi.io.api.LineReader;
import xapi.log.X_Log;
import xapi.log.api.HasLogLevel;
import xapi.log.api.LogLevel;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;
import xapi.shell.service.ShellService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_GC;
import xapi.util.api.SuccessHandler;

@InstanceDefault(implFor = ShellService.class)
@SingletonDefault(implFor = ShellService.class)
public class ShellServiceDefault implements ShellService {

  public ShellServiceDefault() {
  }

  @Override
  public ShellCommand newCommand(String ... cmds) {
    return X_Inject.instance(ShellCommand.class).commands(cmds);
  }

  static {
    new Thread() {
      {
        Runtime.getRuntime().addShutdownHook(this);
      }
      public void run() {
        while(!runningShells.isEmpty()){
          runningShells.remove().destroy();
        }
      }
    };
  }

  private static final Queue<ShellSession> runningShells = new ConcurrentLinkedQueue<>();

//  private static abstract class ScriptProvider extends SingletonProvider<String> {
//    abstract String getScriptName();
//    public String getScriptLocation() {
//      return X_File.getResourceMaybeUnzip(getScriptName(), Thread.currentThread().getContextClassLoader());
//    }
//  }

  @Override
  public ShellSession runInShell(final boolean keepAlive, LineReader stdOut, LineReader stdErr, final String ... cmds) {
    final Moment start = X_Time.now();
    final String[] commands;
    if (keepAlive) {
      final String sh = X_File.unzippedResourcePath("xapi/sh.sh", null);
      commands = new String[]{"sh", "-ac", sh};
    } else {
      commands = cmds;
    }
    final ShellCommand command = X_Inject.instance(ShellCommand.class)
        .commands(commands);
    if (stdOut instanceof HasLogLevel) {
        final LogLevel level = ((HasLogLevel) stdOut).getLogLevel();
        command.setStdOutLevel(level);
    }
    if (stdErr instanceof HasLogLevel) {
        final LogLevel level = ((HasLogLevel) stdErr).getLogLevel();
        command.setStdErrLevel(level);
    }
    final ShellSession runningShell = command.run(new SuccessHandler<ShellSession>() {
            @Override
            public void onSuccess(ShellSession t) {
              X_Log.trace(getClass(), "Shell still running?", t.isRunning());
            }
      }, null);
    X_Log.debug(getClass(), "Time create shell command", X_Time.difference(start));
    if (stdOut != null) {
      runningShell.stdOut(stdOut);
    }
    if (stdErr != null) {
      runningShell.stdErr(stdErr);
    }
    X_Time.runLater(new Runnable() {
      @Override
      public void run() {
        if (keepAlive) {
          for (String cmd : cmds) {
            runningShell.stdIn(cmd);
          }
        }
        X_Log.debug(getClass(), "Time to send shell command", X_Time.difference(start));
        // release thread
        X_Time.trySleep(0, 1000);
        // periodically clean up trash object
        runCleanup();
      }
    });
    return runningShell;
  }

  protected void runCleanup() {
    for (
        Iterator<ShellSession> iter = runningShells.iterator();
        iter.hasNext(); ) {
      ShellSession next = iter.next();
      if (!next.isRunning()) {
        X_Log.trace(getClass(), "Recycle shell that is no longer running", next);
        X_GC.destroy(ShellSession.class, next);
        iter.remove();
      }
    }
  }

}
