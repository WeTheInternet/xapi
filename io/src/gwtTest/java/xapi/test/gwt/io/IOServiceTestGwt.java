/**
 *
 */
package xapi.test.gwt.io;

import org.junit.Ignore;
import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.gwt.io.IOServiceGwt;
import xapi.io.api.DelegatingIOCallback;
import xapi.io.api.IORequest;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Util;
import xapi.util.api.Pointer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.http.client.*;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class IOServiceTestGwt extends GWTTestCase{

    protected IOService service() {
      return new IOServiceGwt() {
        @Override
        protected RequestBuilder newRequest(final Method method, final String url) {
          final RequestBuilder request = new RequestBuilder(method, url) {
            /**
             * @see com.google.gwt.http.client.RequestBuilder#sendRequest(java.lang.String, com.google.gwt.http.client.RequestCallback)
             */
            @Override
            public Request sendRequest(final String requestData, final RequestCallback callback) throws RequestException {
              final XMLHttpRequest xmlHttpRequest = XMLHttpRequest.create();

              xmlHttpRequest.setWithCredentials(true);

              try {
                openSync(xmlHttpRequest, method.toString(), url);
              } catch (final JavaScriptException e) {
                final RequestPermissionException requestPermissionException = new RequestPermissionException(
                    url);
                requestPermissionException.initCause(new RequestException(e.getMessage()));
                throw requestPermissionException;
              }

              doSetHeaders(xmlHttpRequest);

              final Request request = newRequest(xmlHttpRequest, callback);

              // Must set the onreadystatechange handler before calling send().
              xmlHttpRequest.setOnReadyStateChange(new ReadyStateChangeHandler() {
                @Override
                public void onReadyStateChange(final XMLHttpRequest xhr) {
                  if (xhr.getReadyState() == XMLHttpRequest.DONE) {
                    xhr.clearOnReadyStateChange();
                    fireOnResponseReceived(request, callback);
                  }
                }

                private native void fireOnResponseReceived(Request request, RequestCallback callback)
                /*-{
                   request.@com.google.gwt.http.client.Request::fireOnResponseReceived(*)(callback);
                }-*/;
              });

              try {
                xmlHttpRequest.send(requestData);
              } catch (final JavaScriptException e) {
                throw new RequestException(e.getMessage());
              }

              return request;
            }

            private native Request newRequest(XMLHttpRequest ajax, RequestCallback callback)
            /*-{
              return @com.google.gwt.http.client.Request::new(Lcom/google/gwt/xhr/client/XMLHttpRequest;ILcom/google/gwt/http/client/RequestCallback;)(ajax, 30000, callback);
            }-*/;

            private native void doSetHeaders(XMLHttpRequest ajax)
            /*-{
               this.@com.google.gwt.http.client.RequestBuilder::setHeaders(*)(ajax);
            }-*/;

            private native void openSync(XMLHttpRequest ajax, String method, String url)
            /*-{
               ajax.open(method, url, false);
            }-*/;
          };
          return request;
        }
      };
    }

    public void testRemoveMe() {
      GWT.log(Window.Location.getParameterMap() + "2");
    }

    @Ignore("The online service we are using does not set CORS correctly; disabled until "
        + "we setup our own local test server")
    public void doNotTestGet() {
      final Moment now = X_Time.now();
      final Pointer<Boolean> success = new Pointer<Boolean>(false);
      IORequest<String> response;
      try{
      response = service().get("http://httpbin.org/get", null,
          new DelegatingIOCallback<>(t -> {
        assertNotNull(t.body());
        assertNotSame(0, t.body().length());
        success.set(true);
      }));
      } catch (final Throwable e){
        throw X_Util.rethrow(e);
      }
      response.response();
      assertTrue(success.get());
      System.out.println("Test took "+X_Time.difference(now));
    }

    @Ignore("The online service we are using does not set CORS correctly; disabled until "
        + "we setup our own local test server")
    public void doNotTestPost() {
      final Moment now = X_Time.now();
      final Pointer<Boolean> success = new Pointer<Boolean>(false);
      IORequest<String> response;
      try{
        response = service().post("http://httpbin.org/post",
            "test=success",
            null,
            new DelegatingIOCallback<>(
                t -> {
          assertNotNull(t.body());
          assertNotSame(0, t.body().length());
          final JSONValue asJson = JSONParser.parse(t.body());
          assertEquals("test=success", asJson.isObject().get("data").isString().stringValue());
          success.set(true);
        }));
      } catch (final Throwable e){
        throw X_Util.rethrow(e);
      }
      response.response();
      assertTrue(success.get());
      System.out.println("Test took "+X_Time.difference(now));
    }

    @Ignore("The online service we are using does not set CORS correctly; disabled until "
        + "we setup our own local test server")
//    public void testHeaders() {
    public void doNotTestHeaders() {
      final Moment now = X_Time.now();
      final Pointer<Boolean> success = new Pointer<Boolean>(false);
      IORequest<String> response;
      try{
        final StringDictionary<String> headers = X_Collect.newDictionary();
        headers.setValue("test", "success");
        response = service().get("http://headers.jsontest.com", headers, new DelegatingIOCallback<>(t -> {
          X_Log.error("Got response! ",t.body());
          assertNotNull(t.body());
          assertNotSame(0, t.body().length());
          final JSONObject asJson = JSONParser.parse(t.body()).isObject();
          assertEquals("success", asJson.get("test").isString().stringValue());
          assertEquals("*", t.headers().get("Access-Control-Allow-Origin").at(0));
          success.set(true);
        }));
      } catch (final Throwable e){
        throw X_Util.rethrow(e);
      }
      response.response();
      assertTrue(success.get());
      System.out.println("Test took "+X_Time.difference(now));
    }

    /**
     * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
     */
    @Override
    public String getModuleName() {
      return "xapi.test.gwt.io.TestIO";
    }

}
