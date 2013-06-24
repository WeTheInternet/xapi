package xapi.dev.gwt.gui;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ProcessLog extends Box {

  final JTextArea text;
  final JScrollPane scroller;
  StringBuffer body = new StringBuffer();

public ProcessLog() {
    super(BoxLayout.Y_AXIS);

    text = new JTextArea() {
      @Override
      public boolean getScrollableTracksViewportHeight() {
        return false;
      }

      @Override
      public boolean getScrollableTracksViewportWidth() {
        return false;
      }
    };
    scroller = new JScrollPane(text);
    add(scroller);
    out("Process X_Log");
    // setBounds(new Rectangle(500, 500));
    // set
    // setMaximumSize(new Dimension(500, 2000));
    // text.setMaximumSize(new Dimension(500, 2000));
  }

  HashMap<String,Runnable> onDone = new HashMap<String,Runnable>();
  WeakHashMap<String,Thread> waitThreads = new WeakHashMap<String,Thread>();
  ArrayList<BufferedReader> readerStd = new ArrayList<BufferedReader>();
  ArrayList<BufferedReader> readerErr = new ArrayList<BufferedReader>();
  WeakReference<Thread> bufferReader;

  public synchronized void monitor(final Process handle, final String module) {
    try {
      int state = handle.exitValue();
      out("Process for module "+module+" has already terminated w/ exit code "+state);
      return;
    }catch(IllegalThreadStateException e) {
      //still running
      out("Process for module "+module+" is running; engaging logger...");
    }
    final BufferedReader in = new BufferedReader(new InputStreamReader(handle.getInputStream()))
    , err = new BufferedReader(new InputStreamReader(handle.getErrorStream()));
    readerStd.add(in);
    readerErr.add(err);
    if (onDone.containsKey(module)) {
      onDone.get(module).run();
    }
    onDone.put(module, new Runnable() {
      @Override
      public void run() {
        synchronized(ProcessLog.this) {
          readerStd.remove(in);
          readerErr.remove(err);
        }
        try {
          handle.destroy();
        } catch (Exception e) {
          processStdErr(handle, "Error destroying handle; " + e);
        }
      }
    });

    if (null == bufferReader || null == bufferReader.get() || bufferReader.isEnqueued()) {
      Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
          try {
            while (isRunning()) {
              try {
                BufferedReader[] readers;
                synchronized(ProcessLog.this) {
                  readers = readerStd.toArray(new BufferedReader[readerStd.size()]);
                }
                for (BufferedReader read : readers) {
                  while (read.ready()) {
                    while (read.ready()) {
                      String next = read.readLine();
                      processStdIn(handle, next);
                    }
                    Thread.sleep(1);
                  }
                }
                synchronized(ProcessLog.this) {
                  readers = readerErr.toArray(new BufferedReader[readerErr.size()]);
                }
                for (BufferedReader read : readers) {
                  while (read.ready()) {
                    String next = read.readLine();
                    processStdErr(handle, next);
                  }
                }
                Thread.sleep(50);
              } catch (IOException e) {
                processStdErr(handle, "IOException @ " + Arrays.asList(e.getStackTrace()));
                return;
              } catch (InterruptedException e) {
                bufferReader = null;
                processStdErr(handle, "Interrupted @ " + Arrays.asList(e.getStackTrace()));
                return;
              }

            }
          } finally {
            bufferReader = null;
          }
        }
      };
      Thread ioblocker = new Thread(bufferRunnable);
      ioblocker.start();
      bufferReader = new WeakReference<Thread>(ioblocker);
    }
    Thread waitThread = new Thread(new Runnable() {
      Thread selfRemove = new Thread(new Runnable() {

        @Override
        public void run() {
          out("Destroying module "+module);
          handle.destroy();// make sure our jvm kills any processes we started
        }
      });
      {
        Runtime.getRuntime().addShutdownHook(selfRemove);
      }

      @Override
      public void run() {
        try {
          try {
            int state = handle.exitValue();
            out("Process for module "+module+" has terminated w/ exit code "+state);
            return;
          }catch(IllegalThreadStateException e) {
            //still running
            out("Waiting for process "+module+" to finish...");
          }
          int result = handle.waitFor();
          out("Process "+module+" finished with exit code "+result);
          processCompletion(handle, result);
        } catch (InterruptedException e) {
          processStdErr(handle,
            "Interrupted while blocking on monitored command.  \n" + Arrays.asList(e.getStackTrace()));
          Thread.currentThread().interrupt();
        } finally {
          try {
            if (onDone.containsKey(module)) {
              onDone.remove(module).run();
            }
            if (null != selfRemove) Runtime.getRuntime().removeShutdownHook(selfRemove);
            selfRemove = null;
          } catch (Exception e) {
            e.printStackTrace();
            processStdErr(handle,
              "Error " + e + " while cleaning up process monitor.  \n" + Arrays.asList(e.getStackTrace()));
          }
        }
      }
    });
    waitThread.start();
    waitThreads.put(module, waitThread);

  }

  protected boolean isRunning() {
    synchronized(this) {
      return readerStd.size() > 0 || readerErr.size() > 0;
    }
  }

  protected void processCompletion(Process handle, int result) {

  }

  protected void processStdErr(Process handle, String next) {
    out("[ERROR] " + next);
  }

  private void out(String string) {
    body.append(string + "\n");
    scheduleRedraw();
    System.out.println(string);

  }

  public synchronized void stop(String module) {
    if (bufferReader != null) {
      Thread thread = bufferReader.get();
      bufferReader.clear();
      if (thread != null) thread.interrupt();
      bufferReader = null;
    }
    if (waitThreads.containsKey(module)) {
      waitThreads.remove(module).interrupt();
    }
    if (onDone.containsKey(module)) {
      onDone.remove(module).run();
    }
  }

  private Runnable redraw;

  private void scheduleRedraw() {
    if (redraw == null) {
      redraw = new Runnable() {
        final Rectangle visibleRect = scroller.getVisibleRect();
        private int lastHeight;

        @Override
        public void run() {
          redraw = null;
          boolean autoscroll = lastHeight == 0 || visibleRect.y + scroller.getHeight() == lastHeight;
          text.setText(body.toString());
          invalidate();
          if (autoscroll) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                lastHeight = visibleRect.y = scroller.getHeight() - visibleRect.height + 20;
                scroller.scrollRectToVisible(visibleRect);
              }
            });
          }
        }
      };
      SwingUtilities.invokeLater(redraw);
    }
  }

  protected void processStdIn(Process handle, String next) {
    out("[INFO] " + next);
  }

  public void log(String string, IOException e) {
    if (string != null && string.length() > 0) out(string);
    if (e != null) {
      out(e.toString());
      for (StackTraceElement el : e.getStackTrace()) {
        out(el.toString());
      }
    }
  }

}
