package xapi.shell.impl;

import xapi.collect.X_Collect;
import xapi.collect.fifo.Fifo;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.io.X_IO;
import xapi.io.api.HasLiveness;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.process.X_Process;
import xapi.shell.api.ArgumentProcessor;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.impl.RunOnce;
import xapi.debug.X_Debug;
import xapi.util.api.ErrorHandler;
import xapi.util.api.Pointer;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ShellSessionDefault implements ShellSession, Do {

  Process process;
  public boolean finished;
  private final ShellCommandDefault command;
  private final StringReader onStdErr = new StringReader();
  private final StringReader onStdOut = new StringReader();
  private final Fifo<String> stdIns = X_Collect.newFifo();
  private final Fifo<RemovalHandler> clears = X_Collect.newFifo();
  private volatile SuccessHandler<ShellSession> callback;
  private final ErrorHandler<Throwable> err;
  private final ArgumentProcessor processor;
  private final Moment birth = X_Time.now();
  private final RunOnce once = new RunOnce();

  private boolean normalCompletion;
  PipeOut out;
  private Integer status;
  private LogLevel stdOutLevel = LogLevel.TRACE;
  private LogLevel stdErrLevel = LogLevel.ERROR;

  public ShellSessionDefault(final ShellCommandDefault cmd,
      final ArgumentProcessor argProcessor, final SuccessHandler<ShellSession> onSuccess, final ErrorHandler<Throwable> onError) {
    this.command = cmd;
    this.callback = onSuccess;
    this.err = onError;
    this.processor = argProcessor;
  }

  @Override
  public final void close() {
    done();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void done() {
    final InputStream stdOut;
    final InputStream stdErr;
    synchronized (this) {
      if (process == null) {
        InputStream o = null;
        InputStream e = null;
        try {
          process = command.doRun(processor);
          o = process.getInputStream();
          e = process.getErrorStream();
        } catch (final Throwable ex) {
          X_Log.error(getClass(), "Could not start command " + command.commands(), ex);
          err.onError(ex);
        }
        stdOut = o;
        stdErr = e;
      } else {
        stdOut = null;
        stdErr = null;
        X_Log.warn(getClass(), "Shell command " + command.commands() + " has already been started.");
      }
      notifyAll();
    }
    if (stdOut != null) {
      onStdOut.onStart();
      onStdErr.onStart();
      final HasLiveness check = new HasLiveness() {
        @Override
        public boolean isAlive() {
          return !finished;
        }
      };
      X_IO.drain(getStdOutLevel(), stdOut, onStdOut, check);
      X_IO.drain(getStdErrLevel(), stdErr, onStdErr, check);
    }
    join();
    drainStreams();
    if (status != 0) {
      if (callback instanceof ErrorHandler) {
        ((ErrorHandler) callback).onError(new RuntimeException("Exit status " + status + " for " + command.commands));
      }
      X_Log.error("Exit status", status, "for ", command.commands);
    }
    destroy();
    synchronized (this) {
      notifyAll();
    }
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
  public int block(final long l, final TimeUnit seconds) {
    final Thread waiting = Thread.currentThread();
    X_Process.newThread(() -> {
        synchronized (ShellSessionDefault.this) {
          try {
            ShellSessionDefault.this.wait(seconds.toMillis(l), 0);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            waiting.interrupt();
            return;
          }
        }
        if (status == null) {
          waiting.interrupt();
        }
    }).start();
    return join();
  }

  @Override
  public int join() {
    if (status != null) {
      return status;
    }
    try {
      if (process == null) {
        synchronized (this) {
          if (process == null) {
            wait(10000);
          }
        }
        if (status != null) {
          return status;
        }
      }
      if (process == null) {
        X_Log.warn(getClass(),"Process failed to start after "+X_Time.difference(birth));
      } else {
        X_Log.trace(getClass(), "Joining process",process, "after",X_Time.difference(birth), "uptime");
        X_Log.debug(getClass(), "Joining from",new Throwable());
        return (status = process.waitFor());
      }
    } catch (final InterruptedException e) {
      X_Log.info(getClass(), "Interrupted while joining process",process);
      finished = true;
      try {
        if (normalCompletion) {
          return (status = 0);
        }
      status = -1;
      } finally {
        destroy();
      }
      err.onError(e);
    } finally {
      X_Log.trace(getClass(), "Joined process",process,"after", X_Time.difference(birth)," uptime");
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
        for (final String command : this.command.commands.forEach()) {
          final File f = new File(command);
          if (f.exists()) {
            if (!f.canExecute()) {
              X_Log.info(getClass(), "Setting file",f,"to be executable.  Result: ", f.setExecutable(true, false));
            }
          }
        }
      }
      finished = true;
      drainStreams();
    }
    return status;
  }

  @Override
  public void destroy() {
    try {
      if (status == null) {// don't clobber a real exit status
        status = ShellCommandDefault.STATUS_DESTROYED;
      }
      finished = true;
      // Don't block to notify stdErr and stdOut
      if ("stopping".equals(System.getProperty("xapi.system.state"))) {
        onStdOut.onEnd();
        onStdErr.onEnd();
        // do not notify callbacks: we are shutting down.
        return;
      } else {
        X_Time.runLater(new Runnable() {
          @Override
          public void run() {
            onStdOut.onEnd();
            onStdErr.onEnd();
          }
        });
      }
      finish();
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  protected void drainStreams() {
    try {
      X_Log.trace(getClass(), "Process ended; Waiting for stdErr");
      onStdErr.waitToEnd();
      X_Log.trace(getClass(), "Blocking on stdOut");
      onStdOut.waitToEnd();
      X_Log.trace(getClass(), "Done");
    } catch (final InterruptedException e) {
      Thread.interrupted();
      throw X_Debug.rethrow(e);
    }
  }

  protected void finish () {
    boolean shouldRun;
    synchronized (once) {
      for (final RemovalHandler clear : clears.forEach()) {
        clear.remove();
      }
      clears.clear();
      shouldRun = status == 0 && once.shouldRun(false);
    }
    if (shouldRun) {
      synchronized (once) {
        if (callback != null) {
          callback.onSuccess(this);
        }
        callback = ignored->{};
      }
    }
  }

  @Override
  public void onFinished(In1<ShellSession> handler) {
    synchronized (once) {
      if (!once.hasRun()) {
        final SuccessHandler<ShellSession> oldCallback = callback;
        if (oldCallback == null) {
          callback = handler::in;
        } else {
          callback = self-> {
            oldCallback.onSuccess(self);
            handler.in(self);
          };
        }
        return;
      }
      // we have already run; lets defer the callback to avoid eager race conditions
    }
    X_Time.runLater(handler.provide(this).toRunnable());
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
  public ShellSessionDefault stdOut(final LineReader stdReader) {
    onStdOut.forwardTo(stdReader);
    return this;
  }

  @Override
  public ShellSessionDefault stdErr(final LineReader errReader) {
    onStdErr.forwardTo(errReader);
    return this;
  }

  @Override
  public boolean stdIn(final String string) {
    if (!isRunning()) {
      throw new IllegalStateException("The command "+command.commands()+" is not running to receive " +
            "your input of "+string);
    }
    final boolean immediate = stdIns.isEmpty();
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
  class PipeOut implements Do {
    private final Pointer<Boolean> blocking = new Pointer<Boolean>(false);// we start out with content to push.
    private long timeout = 50;
    public PipeOut() {
    }
    void ping(){
      // Called when more stdIn shows up.  If we're blocking now, don't bother.
      if (blocking.get()) {
        return;
      }
      synchronized (blocking) {
        blocking.notifyAll();
      }
    }
    OutputStream os;
    @Override
    public void done() {
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
            } catch (final InterruptedException e) {
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
            final String line = stdIns.take();
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
            } catch (final IOException e) {
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
      try {
        if (process.isAlive()) {
          process.destroy();
        }
        status = process.waitFor();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
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
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      assert waiting == null || waiting == Thread.currentThread() : "Should not make more than"
          + " one thread wait on a process at once.";
      waiting = Thread.currentThread();
      clears.give(this);
      X_Process.runTimeout(this::remove, (int) unit.toMillis(timeout));
      join();
      return getValue();
    }

    protected abstract T getValue();

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
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

  public LogLevel getStdOutLevel() {
    return stdOutLevel;
  }

  public ShellSessionDefault setStdOutLevel(LogLevel stdOutLevel) {
    this.stdOutLevel = stdOutLevel;
    return this;
  }

  public LogLevel getStdErrLevel() {
    return stdErrLevel;
  }

  public ShellSessionDefault setStdErrLevel(LogLevel stdErrLevel) {
    this.stdErrLevel = stdErrLevel;
    return this;
  }
}
