/**
 *
 */
package xapi.gwt.io;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.api.StringTo.Many;
import xapi.except.NoSuchItem;
import xapi.inject.impl.SingletonProvider;
import xapi.io.IOConstants;
import xapi.io.api.*;
import xapi.io.impl.AbstractIOService;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.platform.GwtPlatform;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Runtime;

import javax.inject.Provider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.http.client.RequestBuilder.Method;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@SingletonOverride(implFor=IOService.class)
@GwtPlatform
public class IOServiceGwt extends AbstractIOService <RequestBuilder> {

  /**
   * @author James X. Nelson (james@wetheinter.net, @james)
   *
   */
  public class IORequestGwt extends AbstractIORequest {

    private Request request;

    @Override
    public void cancel() {
      super.cancel();
      if (request != null) {
        request.cancel();
      }
    }

    @Override
    public String response() {
      return getValue();
    }

    public void setRequest(final Request request) {
      this.request = request;
    }

  }

  protected RequestBuilder newRequest(final Method method, final String url) {
    return new RequestBuilder(method, url);
  }

  @Override
  public IORequest<String> get(final String uri, StringDictionary<String> headers, final IOCallback<IOMessage<String>> callback) {
    final String url = normalize(uri);
    if (callback.isCancelled()) {
      return cancelled;
    }
    headers = normalizeHeaders(headers);
    try {
      final RequestBuilder request = newRequest(RequestBuilder.GET, url);
      headers.forEach(request::setHeader);
      applySettings(request, IOConstants.METHOD_GET);
      final IORequestGwt req = createRequest();
      sendRequest(request, req, callback, url, headers, IOConstants.METHOD_GET, null);
      return req;
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
  public IORequest<String> post(final String uri, final String body, StringDictionary<String> headers,
      final IOCallback<IOMessage<String>> callback) {
    final String url = normalize(uri);
    if (callback.isCancelled()) {
      return cancelled;
    }
    try {
      final RequestBuilder request = newRequest(RequestBuilder.POST, url);
      headers = normalizeHeaders(headers);
      headers.forEach((key, value)-> {
          assert value != null : "Cannot set a null header value for "+key+"; url: "+url;
          request.setHeader(key, value);
      });
      applySettings(request, IOConstants.METHOD_POST);
      final IORequestGwt req = createRequest();
      sendRequest(request, req, callback, url, headers, IOConstants.METHOD_POST, body);
      return req;
    } catch (final Throwable e) {
      callback.onError(e);
      if (X_Runtime.isDebug()) {
        X_Log.warn("IO Error", e);
      }
      return cancelled;
    }
  }

  protected StringDictionary<String> normalizeHeaders(StringDictionary<String> headers) {
    if (headers == null) {
      headers = X_Collect.newDictionary();
    }
    if (!headers.hasKey("X-Gwt-Version")) {
      headers.setValue("X-Gwt-Version", GWT.getPermutationStrongName());
    }
    if (!headers.hasKey("X-Gwt-Module")) {
      headers.setValue("X-Gwt-Module", GWT.getModuleName());
    }
    return headers;
  }

  protected void sendRequest(final RequestBuilder req, final IORequestGwt request, final IOCallback<IOMessage<String>> callback, final String url, final StringDictionary<String> headers, final int method, final String body) {
    final LogLevel logLevel = logLevel();
    request.start();
    try {
      final Request r = req.sendRequest(body, new RequestCallback() {

        @Override
        public void onResponseReceived(final Request req, final Response resp) {
          if (request.isCancelled()) {
            callback.onError(new CancelledException(request));
            return;
          }
          int code = resp.getStatusCode();
          if (code == 404) {
            callback.onError(new NoSuchItem(url));
            return;
          }
          boolean success = code >= 200 && code < 300;
          final Provider<Many<String>> resultHeaders = new SingletonProvider<Many<String>>() {

            @Override
            protected Many<String> initialValue() {
              final Many<String> headers = X_Collect.newStringMultiMap(String.class);
              for (final Header header : resp.getHeaders()) {
                headers.add(header.getName(), header.getValue());
              }
              return headers;
            }
          };
          request.setStatus(code, resp.getStatusText());
          request.setValue(resp.getText());
          request.setResultHeaders(resultHeaders);

          final Moment callbackTime = X_Time.now();
          try {
            final IOMessage<String> msg = new IOMessage<String>() {
              @Override
              public String body() {
                return request.getValue();
              }

              @Override
              public int modifier() {
                return method;
              }

              @Override
              public String url() {
                return url;
              }

              @Override
              public int statusCode() {
                return resp.getStatusCode();
              }

              @Override
              public String statusMessage() {
                return resp.getStatusText();
              };

              @Override
              public StringTo.Many<String> headers() {
                return resultHeaders.get();
              }
            };
            if (success) {
              callback.onSuccess(msg);
            } else {
              callback.onError(new RequestFailed(msg));
            }
          } catch (final Throwable t) {
            X_Log.error("Error invoking IO callback on",callback,"for request",url, t, t.getStackTrace());
            callback.onError(t);
          }
          if (X_Log.loggable(logLevel)) {
            X_Log.log(IOServiceGwt.class, logLevel, "Callback time for ",url,"took",X_Time.difference(callbackTime));
          }

        }

        @Override
        public void onError(final Request request, final Throwable exception) {
          callback.onError(exception);
        }
      });
      request.setRequest(r);
    } catch (final Throwable e) {
      callback.onError(e);
      if (X_Runtime.isDebug()) {
        X_Log.error(getClass(), "Error sending request",url,e);
      }
    }

  }

  protected IORequestGwt createRequest() {
    return new IORequestGwt();
  }

  @Override
  protected native String uriBase()
  /*-{
    if (!$wnd.location.origin) {
      if ($wnd.location.port === 80 || $wnd.location.port === 443) {
        $wnd.location.origin = $wnd.location.host;
      } else {
        $wnd.location.origin = $wnd.location.host + ":" + $wnd.location.port;
      }
    }
    return $wnd.location.origin;
  }-*/;

}
