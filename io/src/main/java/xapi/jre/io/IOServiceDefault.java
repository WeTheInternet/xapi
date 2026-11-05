/**
 *
 */
package xapi.jre.io;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.api.StringTo.Many;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.io.IOConstants;
import xapi.io.X_IO;
import xapi.io.api.*;
import xapi.io.impl.AbstractIOService;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Runtime;
import xapi.util.X_Util;
import xapi.util.api.ReceivesValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@InstanceDefault(implFor=IOService.class)
@SingletonDefault(implFor=IOService.class)
public class IOServiceDefault extends AbstractIOService <URLConnection> {

  /**
   * @author James X. Nelson (james@wetheinter.net, @james)
   *
   */
  public class IORequestDefault extends AbstractIORequest {

    private Thread connectionThread;

    @Override
    public boolean isSuccess() {
      // An HTTP error can have a complete, non-null response body.
      return super.isSuccess() && getStatusCode() < 400;
    }

    @Override
    public void cancel() {
      super.cancel();
      if (connectionThread != null) {
        connectionThread.interrupt();
        connectionThread = null;
        synchronized (this) {
          notifyAll();
        }
      }
    }

    @Override
    public String response() {
      if (!super.isStarted()) {
        synchronized (this) {
          try {
            wait();
          } catch (final InterruptedException e) {
            cancel();
            Thread.currentThread().interrupt();
          }
        }
      }
      if (connectionThread != null) {
        synchronized (connectionThread) {
          try {
            if (connectionThread != null) {
              connectionThread.join();
            }
          } catch (final InterruptedException ignored) {
            cancel();
            Thread.currentThread().interrupt();
          }
          connectionThread = null;
        }
      }
      return getValue();
    }

    public void setConnectionThread(final Thread thread) {
      this.connectionThread = thread;
    }

  }

  @Override
  public IORequest<String> get(final String uri, final StringDictionary<String> headers, final IOCallback<IOMessage<String>> callback) {
    final String url = normalize(uri);
    if (callback.isCancelled()) {
      return cancelled;
    }
    final Moment startUp = X_Time.now();
    try {
      final URL asUrl = new URL(url);
      final URLConnection connect = asUrl.openConnection();
      connect.setDoInput(true);
      connect.setDoOutput(false);
      if (headers != null) {
        headers.forKeys(new ReceivesValue<String>() {
          @Override
          public void set(final String key) {
            final String value = headers.getValue(key);
            connect.setRequestProperty(key, value);
          }
        });
      }
      applySettings(connect, IOConstants.METHOD_GET);
      final LogLevel logLevel = logLevel();
      if (X_Log.loggable(logLevel)) {
        X_Log.log(getClass(), logLevel, "Startup time for ",url,"took",X_Time.difference(startUp));
      }
      final IORequestDefault request = createRequest();
      sendRequest(connect, request, callback, url, headers, null);
      return request;
    } catch (final Throwable e) {
      callback.onError(e);
      if (X_Runtime.isDebug()) {
        X_Log.warn("IO Error", e);
      }
      return cancelled;
    }
  }

  /**
   * @see xapi.io.service.IOService#post(java.lang.String, java.lang.String, xapi.collect.api.StringDictionary, xapi.io.api.IOCallback)
   */
  @Override
  public IORequest<String> post(final String uri, final String body, final StringDictionary<String> headers,
      final IOCallback<IOMessage<String>> callback) {
    final String url = normalize(uri);
    if (callback.isCancelled()) {
      return cancelled;
    }
    final Moment startUp = X_Time.now();
    try {
      final URL asUrl = new URL(url);
      final URLConnection connect = asUrl.openConnection();
      connect.setDoInput(true);
      connect.setDoOutput(true);
      if (headers != null) {
        headers.forKeys(new ReceivesValue<String>() {
          @Override
          public void set(final String key) {
            final String value = headers.getValue(key);
            connect.setRequestProperty(key, value);
          }
        });
      }
      applySettings(connect, IOConstants.METHOD_POST);
      final LogLevel logLevel = logLevel();
      if (X_Log.loggable(logLevel)) {
        X_Log.log(getClass(), logLevel, "Startup time for ",url,"took",X_Time.difference(startUp));
      }
      final IORequestDefault request = createRequest();
      sendRequest(connect, request, callback, url, headers, body);
      return request;
    } catch (final Throwable e) {
      callback.onError(e);
      if (X_Runtime.isDebug()) {
        X_Log.warn("IO Error", e);
      }
      return cancelled;
    }
  }


  protected void sendRequest(final URLConnection connect, final IORequestDefault request, final IOCallback<IOMessage<String>> callback, final String url, final StringDictionary<String> headers, final String body) {
    final LogLevel logLevel = logLevel();
    final Moment before = X_Time.now();
    X_Time.doLaterUnsafe(new DoUnsafe() {
      @Override
      public void doneUnsafe() throws Throwable {
        if (X_Log.loggable(logLevel)) {
          X_Log.log(getClass(), logLevel, "Starting IO for ",url,"took",X_Time.difference(before));
        }
        if (request.isCancelled()) {
          callback.onError(new CancelledException(request));
          return;
        }
        request.setConnectionThread(Thread.currentThread());
        synchronized (request) {
          request.start();
          request.notifyAll();
        }
        InputStream in;
        String res;
        try {
          if (body != null) {
            // We need to send data on the output stream first
            final Moment start = X_Time.now();
            try(
              final OutputStream out = connect.getOutputStream();
            ) {
              in = toStream(body, headers);
              X_IO.drain(out, in);
              if (X_Log.loggable(logLevel)) {
                X_Log.log(getClass(), logLevel, "Sending data for ",url,"took",X_Time.difference(start));
              }
            }
          }
          final Moment start = X_Time.now();
          try {
            in = responseStream(connect);
            try {
              res = drainInput(in, callback);
            } finally {
              in.close();
            }
          } catch (final SocketException e) {
            in = responseStream(connect);
            if (request.isCancelled()) {
              callback.onError(new CancelledException(request));
              return;
            }
            try {
              res = drainInput(in, callback);
            } finally {
              in.close();
            }
          }
          if (X_Log.loggable(logLevel)) {
            X_Log.log(getClass(), logLevel, "Receiving data for ",url,"took",X_Time.difference(start));
          }
          if (request.isCancelled()) {
            callback.onError(new CancelledException(request));
            return;
          }

          final Out1<Many<String>> resultHeaders = Lazy.deferred1(() -> {
              final Many<String> headers = X_Collect.newStringMultiMap(String.class);
              for (final Entry<String, List<String>> entry : connect.getHeaderFields().entrySet()) {
                for (final String value : entry.getValue()) {
                  headers.add(entry.getKey(), value);
                }
              }
              return headers;
            }
          );
          request.setValue(res);
          request.setResultHeaders(resultHeaders);
          if (connect instanceof HttpURLConnection) {
            final int status = ((HttpURLConnection)connect).getResponseCode();
            final String message = ((HttpURLConnection)connect).getResponseMessage();
            request.setStatus(status, message);
          } else {
            request.setStatus(IORequest.STATUS_NOT_HTTP, "Request not using http: "+connect.getClass());
          }

          final IOMessage<String> message = new IOMessage<String>() {
              @Override
              public String body() {
                return request.getValue();
              }

              @Override
              public int modifier() {
                return IOConstants.METHOD_GET;
              }

              @Override
              public String url() {
                return url;
              }

              @Override
              public StringTo.Many<String> headers() {
                return resultHeaders.out1();
              }

              @Override
              public int statusCode() {
                return request.getStatusCode();
              }

              @Override
              public String statusMessage() {
                return request.getStatusText();
              }
          };
          if (message.statusCode() >= 400) {
            // Keep HTTP failures distinct from network/read failures. In particular,
            // callers must not infer 404 merely from FileNotFoundException.
            throw new RequestFailed(message);
          }
          final Moment callbackTime = X_Time.now();
          try {
            callback.onSuccess(message);
          } catch (final Throwable t) {
            X_Log.error("Error invoking IO callback on",callback,"for request",url, t);
            callback.onError(X_Util.unwrap(t));
          }
          if (X_Log.loggable(logLevel)) {
            X_Log.log(getClass(), logLevel, "Callback time for ",url,"took",X_Time.difference(callbackTime));
          }
        } catch (final Throwable t) {
          request.cancel();
          callback.onError(X_Util.unwrap(t));
        } finally {
          request.connectionThread = null;
        }

      }
    });

  }

  private InputStream toStream(final String body, final StringDictionary<String> headers) {
    return X_IO.toStreamUtf8(body);
  }

  private InputStream responseStream(final URLConnection connection) throws IOException {
    if (connection instanceof HttpURLConnection) {
      final HttpURLConnection http = (HttpURLConnection) connection;
      if (http.getResponseCode() >= 400) {
        final InputStream error = http.getErrorStream();
        return error == null ? new ByteArrayInputStream(new byte[0]) : error;
      }
    }
    return connection.getInputStream();
  }

  protected IORequestDefault createRequest() {
    return new IORequestDefault();
  }

  protected String drainInput
    ( final InputStream in, final IOCallback<IOMessage<String>> callback)
    throws IOException {
    try {
      // Model and other length-prefixed protocols need the exact response, including
      // line endings. LineReader callbacks still receive their usual stripped lines.
      final StringBuilder message = new StringBuilder();
      final BufferedReader read = new BufferedReader(new FilterReader(new InputStreamReader(in, StandardCharsets.UTF_8)) {
        @Override
        public int read(final char[] buffer, final int offset, final int length) throws IOException {
          final int count = super.read(buffer, offset, length);
          if (count > 0) {
            message.append(buffer, offset, count);
          }
          return count;
        }
      });
      final LineReader lines = callback instanceof LineReader ? (LineReader) callback : null;
      String line;
      if (lines != null) {
        lines.onStart();
      }
      while ((line = read.readLine())!=null) {
        if (lines != null) {
          lines.onLine(line);
        }
      }
      if (lines != null) {
        lines.onEnd();
      }
      // All done.
      return message.toString();
    } finally {
      in.close();
    }
  }

}
