package xapi.shell.impl;

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
import xapi.shell.api.ShellResult;
import xapi.time.X_Time;
import xapi.time.impl.RunOnce;
import xapi.util.api.ErrorHandler;
import xapi.util.api.Pointer;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

class ShellResultDefault implements ShellResult, Runnable {

  Process process;
  public boolean finished;
  private final StringReader onStdErr = new StringReader();
  private final StringReader onStdOut = new StringReader();

  private final ShellCommandDefault command;
  private final SuccessHandler<ShellResult> callback;
  private final ErrorHandler<Throwable> err;
  private final ArgumentProcessor processor;
  private final double birth = X_Time.now().millis();
  private final RunOnce once = new RunOnce();
  private boolean normalCompletion;

  public ShellResultDefault(ShellCommandDefault cmd, 
      ArgumentProcessor argProcessor, SuccessHandler<ShellResult> onSuccess, ErrorHandler<Throwable> onError) {
    this.command = cmd;
    this.callback = onSuccess;
    this.err = onError;
    this.processor = argProcessor;
  }
  
  @Override
  public synchronized void run() {
    if (process == null) {
      try {
        process = command.doRun(processor);
        final InputStream stdOut = process.getInputStream();
        final InputStream stdErr = process.getErrorStream();
        onStdOut.onStart();
        onStdErr.onStart();
        X_IO.drain(LogLevel.INFO, stdOut, onStdOut, X_Shell.liveChecker(process));
        X_IO.drain(LogLevel.ERROR, stdErr, onStdErr, X_Shell.liveChecker(process));
      } catch (Throwable e) {
        err.onError(e);
        X_Log.error("Could not start command " + command.commands(), e);
      }
    } else {
      X_Log.warn("Shell command " + command.commands() + " has already been started.");
    }
  }

  @Override
  public double birth() {
    return birth;
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
  public synchronized int join() {
    if (status != null)
      return status;
    try {
      return (status = process.waitFor());
    } catch (InterruptedException e) {
      finished = true;
      if (normalCompletion) {
        return status = 0;
      }
      status = -1;
      err.onError(e);
      return status;
    } finally {
      if (status == 126) {
        // The scripts need chmod +x
        X_Log.warn("The script you are trying to run requires chmod +x");
      }
    }
  }

  @Override
  public synchronized void destroy() {
    status = ShellCommandDefault.STATUS_DESTROYED;
    finished = true;
    onStdErr.onEnd();
    onStdOut.onEnd();
    finish();
  }
  
  protected void finish () {
    for (RemovalHandler clear : clears.forEach()) {
      clear.remove();
    }
    clears.clear();
    if (status == 0 && once.shouldRun(false)) {
      if (callback != null)
        callback.onSuccess(this);
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
  public ShellResultDefault stdOut(LineReader stdReader) {
    onStdOut.forwardTo(stdReader);
    return this;
  }

  @Override
  public ShellResultDefault stdErr(LineReader errReader) {
    onStdErr.forwardTo(errReader);
    return this;
  }
  PipeOut out;
  private final Fifo<String> stdIns = X_Collect.newFifo();
  private final Fifo<RemovalHandler> clears = X_Collect.newFifo();
  private Integer status;
  
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
      out.ping();
    }
    return immediate;
  }
  class PipeOut implements Runnable{
    private final Pointer<Boolean> blocking = new Pointer<Boolean>(false);// we start out with content to push.
    private long timeout = 1500;
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
      try {
      while(isRunning()) {
        if (stdIns.isEmpty() || process == null) {
          // go to sleep
          synchronized (blocking) {
            timeout+=100;
            blocking.set(false);
            try {
              blocking.wait(timeout);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              X_Log.error("Shell command $" +
                    command.commands()+" thread interrupted; bailing now.");
              return;
            }
          }
        } else {
          timeout = 500;
          blocking.set(true);
          String line = stdIns.take();
          try {
            if (os == null){
              os = process.getOutputStream();
            }
            if (os == null){
              X_Log.warn("Null output stream  for "+command.commands);
            }
            else {
              OutputStream o = process.getOutputStream();
              o.write((line+"\n").getBytes());
              o.flush();
            }
          } catch (IOException e) {
            X_Log.warn("Command "+command.commands()+" received IO error sending "+line);
            // TODO maybe put command back on stack; though recursion sickness would suck
          } finally {
            blocking.set(false);
          }
          
        }
      }
      if (!stdIns.isEmpty()){
        X_Log.warn("Ended command "+command.commands()+" while stdIn still had data in the buffer:");
        X_Log.warn(stdIns.join(" -- "));
      }
      } finally {
        out = null;
      }
      
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