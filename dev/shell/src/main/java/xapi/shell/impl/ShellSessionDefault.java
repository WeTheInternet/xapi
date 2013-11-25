package xapi.shell.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.io.X_IO;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.process.X_Process;
import xapi.shell.X_Shell;
import xapi.shell.api.ArgumentProcessor;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;
import xapi.shell.service.ShellService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.impl.RunOnce;
import xapi.util.X_Debug;
import xapi.util.api.ErrorHandler;
import xapi.util.api.Pointer;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

class ShellSessionDefault implements ShellSession, Runnable {

  Process process;
  public boolean finished;
  private final ShellCommandDefault command;
  private final StringReader onStdErr = new StringReader();
  private final StringReader onStdOut = new StringReader();
  private final Fifo<String> stdIns = X_Collect.newFifo();
  private final Fifo<RemovalHandler> clears = X_Collect.newFifo();
  private final SuccessHandler<ShellSession> callback;
  private final ErrorHandler<Throwable> err;
  private final ArgumentProcessor processor;
  private final Moment birth = X_Time.now();
  private final RunOnce once = new RunOnce();

  private boolean normalCompletion;
  PipeOut out;
  private Integer status;

  public ShellSessionDefault(ShellCommandDefault cmd, 
      ArgumentProcessor argProcessor, SuccessHandler<ShellSession> onSuccess, ErrorHandler<Throwable> onError) {
    this.command = cmd;
    this.callback = onSuccess;
    this.err = onError;
    this.processor = argProcessor;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void run() {
    synchronized (this) {
      if (process == null) {
        try {
          process = command.doRun(processor);
          final InputStream stdOut = process.getInputStream();
          final InputStream stdErr = process.getErrorStream();
          onStdOut.onStart();
          onStdErr.onStart();
          X_IO.drain(LogLevel.TRACE, stdOut, onStdOut, X_Shell.liveChecker(process));
          X_IO.drain(LogLevel.ERROR, stdErr, onStdErr, X_Shell.liveChecker(process));
        } catch (Throwable e) {
          X_Log.error(getClass(), "Could not start command " + command.commands(), e);
          err.onError(e);
        }
      } else {
        X_Log.warn(getClass(), "Shell command " + command.commands() + " has already been started.");
      }
      notifyAll();
    }
    join();
    if (status == 0) {
      if (callback != null) {
        callback.onSuccess(this);
      } else {
        if (callback instanceof ErrorHandler) {
          ((ErrorHandler)callback).onError(new RuntimeException("Exit status "+status+" for "+command.commands));
        }
        X_Log.error("Exit status",status,"for ",command.commands);
      }
    }
    drainStreams();
    destroy();
  }

  @Override
  public double birth() {
    return birth.millis();
  }

  @Override
  public ShellCommand parent() {
    return command;
  }

  @Override
  public int pid() {
    return 0;
  }

  @Override
  public int join() {
    if (status != null) {
      return status;
    }
    try {
      if (process == null) {
        synchronized (this) {
          wait(10000);
        }
      }
      X_Log.info(getClass(), "Joining process",process, "after",X_Time.difference(birth), "uptime");
      return (status = process.waitFor());
    } catch (InterruptedException e) {
      X_Log.info(getClass(), "Interrupted while joining process",process);
      finished = true;
      if (normalCompletion) {
        return (status = 0);
      }
      status = -1;
      err.onError(e);
    } finally {
      X_Log.info(getClass(), "Joined process",process,"after", X_Time.difference(birth)," uptime");
      if (status == null) {
        if (process == null) {
          status = ShellCommand.STATUS_FAILED;
        } else {
          status = process.exitValue();
        }
        X_Log.warn(getClass(), "Process did not exit normally; status:",status);
      }
      if (status == 126) {
        // The scripts need chmod +x
        X_Log.warn(getClass(), "The script you are trying to run requires chmod +x\n",command.commands);
        X_Log.info(getClass(), "Attempting to make files executable");
        for (String command : this.command.commands.forEach()) {
          File f = new File(command);
          if (f.exists()) {
            if (!f.canExecute()) {
              X_Log.info(getClass(), "Setting file",f,"to be executable.  Result: ", f.setExecutable(true, false));
            }
          }
        }
      }
    }
    return status;
  }

  @Override
  public synchronized void destroy() {
    if (status == null) {// don't clobber a real exit status
      status = ShellCommandDefault.STATUS_DESTROYED;
    }
    finished = true;
    // Don't block to notify stdErr and stdOut
    X_Time.runLater(new Runnable() {
      @Override
      public void run() {
        onStdOut.onEnd();
        onStdErr.onEnd();
      }
    });
    finish();
  }
  
  protected void drainStreams() {
    try {
      X_Log.trace(getClass(), "Process ended; blocking on stdOut");
      onStdOut.waitToEnd();
      X_Log.trace(getClass(), "Waiting for stdErr");
      onStdErr.waitToEnd();
      X_Log.trace(getClass(), "Done");
    } catch (InterruptedException e) {
      Thread.interrupted();
      throw X_Debug.rethrow(e);
    }
  }

  protected void finish () {
    for (RemovalHandler clear : clears.forEach()) {
      clear.remove();
    }
    clears.clear();
    if (status == 0 && once.shouldRun(false)) {
      if (callback != null) {
        callback.onSuccess(this);
      }
    }
  }

  @Override
  public boolean isRunning() {
    return command == null ? false : status == null;
  }

  @Override
  public Future<Integer> exitStatus() {
    return new FutureCommand<Integer>() {
      @Override
      protected Integer getValue() {
        return join();
      }
    };
  }

  @Override
  public ShellSessionDefault stdOut(LineReader stdReader) {
    onStdOut.forwardTo(stdReader);
    return this;
  }

  @Override
  public ShellSessionDefault stdErr(LineReader errReader) {
    onStdErr.forwardTo(errReader);
    return this;
  }
  
  @Override
  public boolean stdIn(String string) {
    if (!isRunning())
      throw new IllegalStateException("The command "+command.commands()+" is not running to receive " +
            "your input of "+string);
    boolean immediate = stdIns.isEmpty();
    stdIns.give(string);
    if (immediate) {
      // maybe have to init 
      synchronized (stdIns) {
        // don't want to init twice!
        if (out == null) {
          out = new PipeOut();
          X_Process.newThread(out).start();
        } else {
          out.ping();
        }
      }
      
    } else {
      if (out == null) {
        X_Log.error(getClass(), "Attempting to send message to closed process, ",string,"will be ignored");
      } else {
        out.ping();
      }
    }
    return immediate;
  }
  class PipeOut implements Runnable{
    private final Pointer<Boolean> blocking = new Pointer<Boolean>(false);// we start out with content to push.
    private long timeout = 50;
    public PipeOut() {
    }
    void ping(){
      // Called when more stdIn shows up.  If we're blocking now, don't bother.
      if (blocking.get())
        return;
      synchronized (blocking) {
        blocking.notify();
      }
    }
    OutputStream os;
    public void run() {
      X_Log.info(getClass(), "Running process", command.commands);
      try {
      while(isRunning()) {
        if (stdIns.isEmpty() || process == null) {
          X_Log.debug(getClass(), "Waiting until process finishes");
          // go to sleep
          synchronized (blocking) {
            if ((timeout+=50) > 5000) {
              timeout = 2000;
            }
            blocking.set(false);
            try {
              blocking.wait(timeout);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              X_Log.error(getClass(), "Shell command $" +
                    command.commands()+" thread interrupted; bailing now.");
              return;
            }
          }
        } else {
          timeout = 50;
          try {
            blocking.set(true);
            String line = stdIns.take();
            X_Log.trace(getClass(), "Sending command to process stdIn",line);
            try {
              if (os == null){
                os = process.getOutputStream();
              }
              if (os == null){
                X_Log.warn(getClass(), "Null output stream  for "+command.commands);
              }
              else {
                os.write((line+"\n").getBytes());
                os.flush();
              }
            } catch (IOException e) {
              X_Log.warn(getClass(), "Command ",command.commands()," received IO error sending ",line,"\n", e);
              // TODO perhaps put command back on stack; though recursion sickness would suck
            }
          }finally {
            blocking.set(false);
          }
          
        }
      }
      if (!stdIns.isEmpty()){
        X_Log.warn(getClass(), "Ended command "+command.commands()+" while stdIn still had data in the buffer:");
        X_Log.warn(stdIns.join(" -- "));
        destroy();
      }
      } finally {
        out = null;
        X_Log.info(getClass(), "Finished process", command.commands);
      }
      status = process.exitValue();
    };
  }

  abstract class FutureCommand<T> implements Future<T>, RemovalHandler {
    @Override
    public T get() throws InterruptedException, ExecutionException {
      join();
      return getValue();
    }

    @Override
    public void remove() {
      if (waiting != null && isRunning()) {
        waiting.interrupt();
        waiting = null;
        clears.remove(this);
      }
    }

    Thread waiting;

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      assert waiting == null || waiting == Thread.currentThread() : "Should not make more than"
          + " one thread wait on a process at once.";
      waiting = Thread.currentThread();
      clears.give(this);
      X_Process.runTimeout(new Runnable() {
        @Override
        public void run() {
          remove();
        }
      }, (int) unit.toMillis(timeout));
      join();
      return getValue();
    }

    protected abstract T getValue();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      try {
        destroy();
      } finally {
        if (waiting != null) {
          waiting.interrupt();
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean isCancelled() {
      return ShellCommandDefault.STATUS_DESTROYED.equals(status);
    }

    @Override
    public boolean isDone() {
      return !isRunning();
    }
  }

}