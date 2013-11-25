package xapi.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.inject.Provider;

import xapi.collect.impl.SimpleFifo;
import xapi.inject.X_Inject;
import xapi.io.api.HasLiveness;
import xapi.io.api.IOMessage;
import xapi.io.api.StringReader;
import xapi.io.impl.IOCallbackDefault;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Util;
import xapi.util.api.ErrorHandler;

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
        boolean log = info != null && X_Log.loggable(info);
        int delay = 50;
        int read = 1;
        int loops = 20000;
        
        try {
          top: while (read >= 0 && loops-- > 0) {
            Moment start = X_Time.now();
            do {
              int avail = in.available();
              if (avail == 0) {
                // Maybe process is dead...
                if (!liveCheck.isAlive()){
                  X_Log.debug(getClass(), "Stream not alive; bailing");
                  read = -1;
                  break top;
                }
              }
              byte[] bytes = new byte[Math.min(1024,avail)];
              read = in.read(bytes);
              X_Log.debug(getClass(), "Got", read,"bytes from input stream", this);
              if (read > 0){
                delay = 20;
                buffer.write(bytes, 0, read);
                bytes = null;
                String asStr = new String(buffer.toByteArray(), "UTF-8");
                X_Log.trace(getClass(), "Read", asStr,"from input stream");
                if (log && asStr.trim().length()>0){
                  X_Log.log(info, new SimpleFifo<Object>().give(getClass()).give(asStr));
                }
              } 
              else{
                if (read == -1) {
                  break top;
                }
                X_Time.trySleep(delay*=2,0);
                break;
              }
            } while (X_Time.now().millis() - start.millis() < 100);
            X_Time.trySleep(5, 0);
          }
          if (read < 1) {
            String res = new String(buffer.toByteArray(), "UTF-8");
            successHandler.onLine(res);
          }
          else {
            throw new RuntimeException("Input stream not cleared "+read+"; left: `"+new String(buffer.toByteArray())+"`");
          }
        } catch (Exception e) {
          if (successHandler instanceof ErrorHandler) {
            ((ErrorHandler)successHandler).onError(e);
          }
          X_Log.error(getClass(), "Error draining input stream", info, in, e);
        } finally {
          X_Log.trace(getClass(), "Finished blocking", this);
          successHandler.onEnd();
          close(in);
        }
      }
    });
  }

  private static void start(Runnable runnable) {
    new Thread(runnable).start();
  }

  public static void close(InputStream in) {
    try {
      in.close();
    } catch (IOException ignored){}
  }

  public static boolean isOffline() {
    final boolean[] failure = new boolean[]{false};
    getIOService().get("http://google.com", null, new IOCallbackDefault<IOMessage<String>>() {
      @Override
      public void onError(Throwable e) {
        Throwable unwrapped = X_Util.unwrap(e);
        if (unwrapped instanceof UnknownHostException)
          failure[0] = true;
        else if (unwrapped instanceof SocketException)
          failure[0] = true;
        else {
          e.printStackTrace();
          X_Util.rethrow(e);
        }
      }
    });
    return failure[0];
  }

  public static void drain(OutputStream out, InputStream in) throws IOException {
    int size = 4096;
    byte[] buffer = new byte[size];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      if (read == 0) {
        try {
          Thread.sleep(0, 10000);
        } catch (InterruptedException e) {
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
  

}
