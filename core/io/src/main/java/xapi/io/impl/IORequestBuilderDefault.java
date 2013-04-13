package xapi.io.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.Dictionary;
import xapi.collect.api.StringDictionary;
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
import static xapi.io.IOConstants.*;
import static xapi.collect.X_Collect.*;

public class IORequestBuilderDefault <V> implements IORequestBuilder<V>{


  private int modifier;
  private String url;
  private ConvertsValue<String,V> deserializer;
  private ConvertsValue<V, String> serializer;
  private StringDictionary<String> headers = initHeaders();
  private V value;

  @Override
  public IORequest<V> send(final SuccessHandler<V> handler) {
    // Take a snapshot of the builder values, in case user reuses it.
    final int modifer = this.modifier;
    final String url = this.url;
    final ConvertsValue<String,V> deserializer = this.deserializer;
    final ConvertsValue<V, String> serializer = this.serializer;
    final StringDictionary<String> headerCopy = initHeaders();
    copyDictionary(this.headers, headerCopy);
    final Pointer<V> result = new Pointer<V>(value);
    final Pointer<Throwable> failure = new Pointer<Throwable>(null);
    final IOService service = service();

    class IO implements IORequest<V> {
      boolean pending = true, cancel = false;
      @Override
      public boolean isPending() {
        return pending;
      }

      @Override
      public boolean isSuccess() {
        return !(pending || cancel) && failure.get() == null;
      }

      @Override
      public void cancel() {
        cancel = true;
      }

      @Override
      public V response() {
        return result.get();
      }

      @Override
      public Dictionary<String,String> headers() {
        return headerCopy;
      }
    };
    final IO io = new IO();

    IOCallback<IOMessage<String>> callback = new IOCallback<IOMessage<String>>() {

      @Override
      public void onSuccess(IOMessage<String> t) {
        io.pending = false;
        if (io.cancel) {
          if (X_Runtime.isDebug()) {
            X_Log.trace("Ignoring cancelled message", t.url(), t.body());
          }
          return;
        }
        if (deserializer == null) {
          assert X_String.isEmpty(t.body()) : "Non-null response without a " +
          		"deserializer instance for "+url+"\n Response: "+t.body();
        } else {
          result.set(deserializer.convert(t.body()));
        }
        if (handler != null)
          handler.onSuccess(result.get());
      }

      @Override
      @SuppressWarnings("unchecked")
      public void onError(Throwable e) {
        failure.set(e);
        io.pending = false;
        if (handler instanceof ErrorHandler) {
          ((ErrorHandler<Throwable>)handler).onError(e);
        }
      }

      @Override
      public void onCancel() {
        io.cancel();
      }

      @Override
      public boolean isCancelled() {
        return io.cancel;
      }

    };

    // perform the actual send.
    switch (modifer) {
    case METHOD_DELETE:
      service.delete(url, headerCopy, callback);
      break;
    case METHOD_GET:
      service.get(url, headerCopy, callback);
      break;
    case METHOD_POST:
      service.post(url, serializer.convert(value), headerCopy, callback);
      break;
    case METHOD_PUT:
      service.put(url, toBinary(serializer.convert(value)), headerCopy, callback);
      break;
    case METHOD_HEAD:
    case METHOD_PATCH:
      throw new NotYetImplemented("");
    }

    return io;
  }

  protected byte[] toBinary(String convert) {
    return X_String.getBytes(convert);
  }

  protected StringDictionary<String> initHeaders() {
    return newStringDictionary();
  }

  @Override
  public IOService service() {
    return X_IO.getIOService();
  }

  @Override
  public IORequestBuilder<V> setDeserializer(ConvertsValue<String,V> deserializer) {
    this.deserializer = deserializer;
    return this;
  }

  @Override
  public IORequestBuilder<V> setSerializer(ConvertsValue<V,String> serializer) {
    this.serializer = serializer;
    return this;
  }

  @Override
  public IORequestBuilder<V> setUrl(String url) {
    this.url = url;
    return this;
  }

  @Override
  public IORequestBuilder<V> setHeader(String header, String value) {
    headers.setValue(header, value);
    return this;
  }

  @Override
  public IORequestBuilder<V> setModifier(int modifier) {
    this.modifier = modifier;
    return this;
  }

  @Override
  public IORequestBuilder<V> setValue(V value) {
    this.value = value;
    return this;
  }

}
