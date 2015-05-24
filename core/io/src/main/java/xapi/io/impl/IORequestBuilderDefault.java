package xapi.io.impl;

import static xapi.collect.X_Collect.copyDictionary;
import static xapi.collect.X_Collect.newDictionary;
import static xapi.io.IOConstants.METHOD_DELETE;
import static xapi.io.IOConstants.METHOD_GET;
import static xapi.io.IOConstants.METHOD_HEAD;
import static xapi.io.IOConstants.METHOD_PATCH;
import static xapi.io.IOConstants.METHOD_POST;
import static xapi.io.IOConstants.METHOD_PUT;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo.Many;
import xapi.except.NotYetImplemented;
import xapi.io.X_IO;
import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.IORequest;
import xapi.io.api.IORequestBuilder;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.util.X_Runtime;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ErrorHandler;
import xapi.util.api.Pointer;
import xapi.util.api.SuccessHandler;

public class IORequestBuilderDefault <Out> implements IORequestBuilder<Out>{


  private int modifier;
  private String url;
  private ConvertsValue<String,Out> deserializer;
  private ConvertsValue<Out, String> serializer;
  private final StringDictionary<String> headers = initHeaders();
  private Out value;


  protected static class IO<Out> implements IORequest<Out> {

    boolean pending = true, cancel = false;
    private IORequest<String> request;
    private final Pointer<Out> response;
    private final Pointer<Throwable> error;
    private String statusText;
    private int statusCode;
    public IO(final Pointer<Out> response, final Pointer<Throwable> error) {
      this.response = response;
      this.error = error;
    }

    @Override
    public boolean isPending() {
      return pending;
    }

    @Override
    public boolean isSuccess() {
      if (request == null) {
        return false;
      }
      return !(pending || cancel) && error.get() == null && request.isSuccess();
    }

    @Override
    public void cancel() {
      cancel = true;
      pending = false;
      request.cancel();
    }

    @Override
    public Out response() {
      try {
        return response.get();
      } finally {
        pending = false;
      }
    }

    @Override
    public Many<String> headers() {
      return request.headers();
    }

    /**
     * @param request -> set request
     */
    public void setRequest(final IORequest<String> request) {
      this.request = request;
      if (cancel) {
        request.cancel();
      }
    }

    /**
     * @see xapi.io.api.IORequest#getStatusCode()
     */
    @Override
    public int getStatusCode() {
      return statusCode;
    }

    /**
     * @see xapi.io.api.IORequest#getStatusText()
     */
    @Override
    public String getStatusText() {
      return statusText;
    }

  };

  @Override
  public IORequest<Out> send(final SuccessHandler<Out> handler) {
    // Take a snapshot of the builder values, in case user reuses it.
    final int modifer = this.modifier;
    final String url = this.url;
    final ConvertsValue<Out, String> serializer = this.serializer;
    final StringDictionary<String> headerCopy = initHeaders();
    copyDictionary(this.headers, headerCopy);
    final Pointer<Out> result = new Pointer<Out>(value);
    final Pointer<Throwable> failure = new Pointer<Throwable>(null);
    final IOService service = service();

    final IO<Out> request = createIO(result, failure);
    final IOCallback<IOMessage<String>> callback = createCallback(request, result, failure, deserializer, handler);

    final IORequest<String> io;
    // perform the actual send.
    switch (modifer) {
    case METHOD_DELETE:
      throw new NotYetImplemented("Method DELETE not yet implemented");
//      service.delete(url, headerCopy, callback);
//      break;
    case METHOD_GET:
      io = service.get(url, headerCopy, callback);
      break;
    case METHOD_POST:
      throw new NotYetImplemented("Method POST not yet implemented");
//      service.post(url, serializer.convert(value), headerCopy, callback);
//      break;
    case METHOD_PUT:
      throw new NotYetImplemented("Method PUT not yet implemented");
//      service.put(url, toBinary(serializer.convert(value)), headerCopy, callback);
//      break;
    case METHOD_HEAD:
      throw new NotYetImplemented("Method HEAD not yet implemented");
    case METHOD_PATCH:
      throw new NotYetImplemented("Method PATCH not yet implemented");
    default:
      throw new UnsupportedOperationException("Unknown request type "+modifer);
    }
    request.setRequest(io);
    return request;
  }

  private IOCallback<IOMessage<String>> createCallback(final IO<Out> request, final Pointer<Out> result,
      final Pointer<Throwable> failure, final ConvertsValue<String, Out> deserializer, final SuccessHandler<Out> handler) {
    return new IOCallback<IOMessage<String>>() {

      private boolean cancel = false;
      @Override
      public void onSuccess(final IOMessage<String> t) {
        if (cancel) {
          if (X_Runtime.isDebug()) {
            X_Log.trace("Ignoring cancelled message", t.url(), t.body());
          }
          return;
        }
        request.pending = false;
        request.statusCode = t.statusCode();
        request.statusText = t.statusMessage();
        if (deserializer == null) {
          assert X_String.isEmpty(t.body()) : "Non-null response without a " +
              "deserializer instance for "+url+"\n Response: "+t.body();
        } else {
          result.set(deserializer.convert(t.body()));
        }
        if (handler != null) {
          handler.onSuccess(result.get());
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      public void onError(final Throwable e) {
        failure.set(e);
        request.pending = false;
        if (handler instanceof ErrorHandler) {
          ((ErrorHandler<Throwable>)handler).onError(e);
        }
      }

      @Override
      public void onCancel() {
        cancel = true;
        request.cancel();
      }

      @Override
      public boolean isCancelled() {
        return cancel;
      }

    };
  }

  protected IO<Out> createIO(final Pointer<Out> result, final Pointer<Throwable> failure) {
    return new IO<Out>(result, failure);
  }

  protected byte[] toBinary(final String convert) {
    return X_String.getBytes(convert);
  }

  protected StringDictionary<String> initHeaders() {
    return newDictionary();
  }

  @Override
  public IOService service() {
    return X_IO.getIOService();
  }

  @Override
  public IORequestBuilder<Out> setDeserializer(final ConvertsValue<String,Out> deserializer) {
    this.deserializer = deserializer;
    return this;
  }

  @Override
  public IORequestBuilder<Out> setSerializer(final ConvertsValue<Out,String> serializer) {
    this.serializer = serializer;
    return this;
  }

  @Override
  public IORequestBuilder<Out> setUrl(final String url) {
    this.url = url;
    return this;
  }

  @Override
  public IORequestBuilder<Out> setHeader(final String header, final String value) {
    headers.setValue(header, value);
    return this;
  }

  @Override
  public IORequestBuilder<Out> setModifier(final int modifier) {
    this.modifier = modifier;
    return this;
  }

  @Override
  public IORequestBuilder<Out> setValue(final Out value) {
    this.value = value;
    return this;
  }

}
