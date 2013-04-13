package xapi.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Provider;

import xapi.inject.X_Inject;
import xapi.io.api.HasLiveness;
import xapi.io.api.StringReader;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.time.X_Time;
import xapi.time.api.Moment;
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
      X_Log.trace("Trying to drain a dead process", liveCheck);
      return;
    }
    start(new Runnable() {
      @Override
      public void run() {
        boolean log = info != null && X_Log.loggable(info);
        int delay = 50;
        int read = 1;
        int loops = 20000;
        
        try {
          top: while (read >= 0 && loops-- > 0) {
            X_Time.trySleep(5, 0);
            Moment start = X_Time.now();
            do {
              int avail = in.available();
              if (avail == 0) {
                // Maybe process is dead...
                if (!liveCheck.isAlive()){
                  X_Log.debug("Stream not alive; bailing");
                  break top;
                }
              }
              byte[] bytes = new byte[Math.min(1024,avail)];
              read = in.read(bytes);
              if (read > 0){
                delay = 20;
                buffer.write(bytes, 0, read);
                bytes = null;
                String asStr = new String(buffer.toByteArray(), "UTF-8");
                if (log && asStr.trim().length()>0){
                  X_Log.log(info, asStr);
                }
              } 
              else{
                X_Time.trySleep(delay*=2,0);
                break;
              }
            } while (X_Time.now().millis() - start.millis() < 100);
          }
          if (read < 1)
            successHandler.onLine(new String(buffer.toByteArray(), "UTF-8"));
          else {
            throw new RuntimeException("Input stream not cleared "+read+"; left: |"+new String(buffer.toByteArray())+"|");
          }
        } catch (Exception e) {
          if (successHandler instanceof ErrorHandler) {
            ((ErrorHandler)successHandler).onError(e);
          } else {
            X_Log.error("Error draining input stream", info, in, e);
          }
        } finally {
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
  

}
