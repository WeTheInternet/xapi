package xapi.shell.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.inject.X_Inject;
import xapi.io.api.LineReader;
import xapi.log.X_Log;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellResult;
import xapi.shell.service.ShellService;
import xapi.time.X_Time;
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

  private static ShellResult runningShell; //TODO use a map with key by user...
//  private static abstract class ScriptProvider extends SingletonProvider<String> {
//    abstract String getScriptName();
//    public String getScriptLocation() {
//      return X_Shell.getResourceMaybeUnzip(getScriptName(), Thread.currentThread().getContextClassLoader());
//      
//    }
//  }
  
  @Override
  public ShellResult runInShell(final String cmd, LineReader stdOut, LineReader stdErr) {
    if (runningShell != null && !runningShell.isRunning()) {
      X_Log.trace("Recycle shell that is no longer running");
      X_GC.destroy(ShellResult.class, runningShell);
      runningShell = null;
    }
    if (runningShell == null) {
      String terminal = X_Shell.getResourceMaybeUnzip("xapi/sh.sh", null);
      runningShell = X_Inject.instance(ShellCommand.class)
          .commands("sh","-ac",terminal)
          .run(new SuccessHandler<ShellResult>() {
              @Override
              public void onSuccess(ShellResult t) {
                X_Log.info("success!", t.isRunning());
              }
      }, null);
    }
    if (stdOut != null)
      runningShell.stdOut(stdOut);
    if (stdErr != null)
      runningShell.stdErr(stdErr);
    X_Time.runLater(new Runnable() {
      @Override
      public void run() {
        runningShell.stdIn(cmd);
      }
    });
    return runningShell;
  }
  
}
