package xapi.io;

import xapi.collect.impl.SimpleFifo;
import xapi.fu.has.HasSize;
import xapi.inject.X_Inject;
import xapi.io.api.DelegatingInputStream;
import xapi.io.api.DelegatingOutputStream;
import xapi.io.api.HasLiveness;
import xapi.io.api.IOMessage;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.io.impl.IOCallbackDefault;
import xapi.io.impl.StringBufferOutputStream;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Debug;
import xapi.util.X_Util;
import xapi.util.api.ErrorHandler;

import javax.inject.Provider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static xapi.io.api.StreamDelegate.fromLineReader;

public class X_IO {

  private static final Provider<IOService> service = X_Inject.singletonLazy(IOService.class);

  public static IOService getIOService() {
    return service.get();
  }

  public static void drain(final LogLevel info, final InputStream in,
      final StringReader successHandler, final HasLiveness liveCheck) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
    if (!liveCheck.isAlive()) {
      X_Log.trace(X_IO.class, "Trying to drain a dead process", liveCheck);
      return;
    }
    start(new Runnable() {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void run() {
        final boolean log = info != null && X_Log.loggable(info), trace = X_Log.loggable(LogLevel.DEBUG);
        int delay = 20;
        int read = 1;
        int loops = 20000;
        final Moment birth = X_Time.now();
        try {
          boolean hadBytes = false;
          top: while (read >= 0 && loops-- > 0) {
            final Moment start = X_Time.now();
            do {
              final int avail = in.available();
              if (avail == 0) {
                // Maybe process is dead...
                if (!liveCheck.isAlive()){
                  X_Log.debug(getClass(), "Stream not alive; completing after ",X_Time.difference(birth));
                  read = -1;
                  break top;
                }
              }
              byte[] bytes = new byte[Math.min(4096,avail)];
              if (trace) {
                X_Log.log(info, new SimpleFifo<Object>().give(getClass())
                    .give("before read")
                    .give(X_Time.difference(birth))
                );
              }
              read = in.read(bytes);
              if (trace) {
                X_Log.log(info, new SimpleFifo<Object>().give(getClass())
                    .give("after read")
                    .give(X_Time.difference(birth))
                    .give("delay: "+delay)
                    );
              }
              System.out.flush();
              System.err.flush();
              if (read > 0){
                delay = 20;
                buffer.write(bytes, 0, read);
                hadBytes = true;
                bytes = null;
                if (log){
                  final String asStr = new String(buffer.toByteArray(), "UTF-8");
                  X_Log.log(info, new SimpleFifo<Object>().give(getClass()).give(asStr));
                }
              }
              else{
                if (hadBytes) {
                  hadBytes = false;
                  final String asStr = new String(buffer.toByteArray(), "UTF-8");
                  sendString(successHandler, asStr);
                  buffer.reset();
                }
                if (read == -1) {
                  X_Log.debug(getClass(), "read returned -1");
                  break top;
                }
                break;
              }
            } while (X_Time.isFuture(start.millis() + 100));
            synchronized (liveCheck) {
              delay = delay < 1000 ? delay << 1 : delay > 2000 ? 2000 : delay + 250;
              liveCheck.wait(delay,0);
            }
          }
          if (buffer.size() > 0) {
            final String res = new String(buffer.toByteArray(), "UTF-8");
            sendString(successHandler, res);
            buffer.reset();
          }
          else if (read != -1){
            throw new RuntimeException("Input stream not cleared "+read+"; left: `"+new String(buffer.toByteArray())+"`");
          }
        } catch (final Exception e) {
          if (successHandler instanceof ErrorHandler) {
            ((ErrorHandler)successHandler).onError(e);
          }
          X_Log.error(getClass(), "Error draining input stream", info, in, e);
        } finally {
          X_Log.debug(getClass(), "Finished blocking", this);
          successHandler.onEnd();
          close(in);
        }
      }
    });
  }

  protected static void sendString(final StringReader successHandler, String res) {
    res = res.replaceAll("\r\n", "\n").replace('\r', '\n');
    int pos = 0, ind = res.indexOf('\n');
    while (ind > -1) {
      successHandler.onLine(res.substring(pos, ++ind));
      pos = ind;
      ind = res.indexOf('\n', pos);
    }
    successHandler.onLine(res.substring(pos));
  }

  private static void start(final Runnable runnable) {
    new Thread(runnable).start();
  }

  public static void close(final InputStream in) {
    try {
      in.close();
    } catch (final IOException ignored){ignored.printStackTrace();}
  }

  public static boolean isOffline() {
    final boolean[] failure = new boolean[]{false};
    getIOService().get("http://google.com", null, new IOCallbackDefault<IOMessage<String>>() {
      @Override
      public void onError(final Throwable e) {
        final Throwable unwrapped = X_Util.unwrap(e);
        if (unwrapped instanceof UnknownHostException) {
          failure[0] = true;
        } else if (unwrapped instanceof SocketException) {
          failure[0] = true;
        } else {
          e.printStackTrace();
          X_Util.rethrow(e);
        }
      }
    });
    return failure[0];
  }

  public static void drain(final OutputStream out, final InputStream in) throws IOException {
    int size = 4096;
    byte[] buffer = new byte[size];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      if (read == 0) {
        try {
          Thread.sleep(0, 10000);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          X_Log.warn("Interrupted while draining input stream",in,"to output stream",out);
          return;
        }
        continue;
      }
      out.write(buffer, 0, read);
      if (size < 0x10000) {
        size <<= 0;
      }
      buffer = new byte[size];
    }
  }

  public static String toStringUtf8(final InputStream in) throws IOException {
    try (
        final StringBufferOutputStream b = new StringBufferOutputStream()
    ) {
      drain(b, in);
      return b.toString();
    }
  }

  public static byte[] toByteArray(final InputStream in) throws IOException {
    try (
        final ByteArrayOutputStream b =
            in instanceof HasSize ?
                new ByteArrayOutputStream( ((HasSize)in).size() ) :
                new ByteArrayOutputStream()
    ) {
      drain(b, in);
      return b.toByteArray();
    }
  }

  public static InputStream toStreamUtf8(final String in) {
    try {
      return new ByteArrayInputStream(in.getBytes("utf-8"));
    } catch (final UnsupportedEncodingException e) {
      X_Debug.debug(e);
      return new ByteArrayInputStream(in.getBytes());
    }
  }

  public static InputStream toStream(final String in, final String charset) {
    try {
      return new ByteArrayInputStream(in.getBytes(charset));
    } catch (final UnsupportedEncodingException e) {
      X_Debug.debug(e);
      return new ByteArrayInputStream(in.getBytes());
    }
  }

    public static InputStream spy(InputStream in, LineReader lineReader) {
        return new DelegatingInputStream(in, fromLineReader(lineReader));
    }

    public static OutputStream spy(OutputStream in, LineReader lineReader) {
        return new DelegatingOutputStream(in, fromLineReader(lineReader));
    }
}
