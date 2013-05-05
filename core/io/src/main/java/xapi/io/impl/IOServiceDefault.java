package xapi.io.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.io.IOConstants;
import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.IORequestBuilder;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Runtime;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;
import xapi.util.api.RemovalHandler;
import static xapi.collect.X_Collect.newClassMap;

@InstanceDefault(implFor=IOService.class)
@SingletonDefault(implFor=IOService.class)
public class IOServiceDefault implements IOService{

  public IOServiceDefault() {
  }

  @SuppressWarnings("unchecked")
  private static final Class<ConvertsValue<?,String>> SERIAL_CLASS
    = Class.class.cast(ConvertsValue.class);

  @SuppressWarnings("unchecked")
  private static final Class<ConvertsValue<String, ?>> DESERIAL_CLASS
    = Class.class.cast(ConvertsValue.class);

  private final ClassTo<ConvertsValue<?, String>> serializers = newClassMap(SERIAL_CLASS);

  private final ClassTo<ConvertsValue<String, ?>> deserializers = newClassMap(DESERIAL_CLASS);

  @Override
  @SuppressWarnings("unchecked") // The class key is always checked...
  public <V> IORequestBuilder<V> request(Class<V> classLit, String id) {
    return new IORequestBuilderDefault<V>()
      .setUrl(toUrl(classLit, id))
      .setDeserializer(ConvertsValue.class.cast(deserializers.get(classLit)))
      .setSerializer(ConvertsValue.class.cast(serializers.get(classLit)))
      ;
  }

  @Override
  public <V> RemovalHandler registerParser(final Class<V> classLit,
    final ConvertsValue<V,String> serializer,
    final ConvertsValue<String,V> deserializer
    ) {
    final ConvertsValue<?,String> currentSerializer = serializers.put(classLit, serializer);
    final ConvertsValue<String,?> currentDeserialize = deserializers.put(classLit, deserializer);
    return new RemovalHandler() {
      @Override
      public void remove() {
        // check before set.
        if (serializers.get(classLit) == serializer) {
          serializers.put(classLit, currentSerializer);
        }
        if (deserializers.get(classLit) == deserializer) {
          deserializers.put(classLit, currentDeserialize);
        }
      }
    };
  }

  @Override
  public void put(String url, byte[] body, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback) {

  }

  @Override
  public void get(String uri, final StringDictionary<String> headers, IOCallback<IOMessage<String>> callback) {
    final String url = normalize(uri);
    if (callback.isCancelled())
      return;
    try {
      URL asUrl = new URL(url);
      final URLConnection connect = asUrl.openConnection();
      connect.setDoInput(true);
      connect.setDoOutput(false);
      if (headers != null)
      headers.forKeys(new ReceivesValue<String>() {
        @Override
        public void set(String key) {
          String value = headers.getValue(key);
          connect.setRequestProperty(key, value);
        }
      });
      applySettings(connect, IOConstants.METHOD_GET);
      InputStream in = connect.getInputStream();
      final String result;
      try {
        result = drainInput(in, callback);
      } finally {
        in.close();
      }

      callback.onSuccess(new IOMessage<String>() {
        @Override
        public String body() {
          return result;
        }

        @Override
        public int modifier() {
          return IOConstants.METHOD_GET;
        }

        @Override
        public String url() {
          return url;
        }

        StringDictionary<String> headers;

        @Override
        public StringDictionary<String> headers() {
          if (headers == null) {
            headers = X_Collect.newStringDictionary();
//            connect.getHeaderFields();
          }
          return headers;
        }
      });
    } catch (Throwable e) {
      callback.onError(e);
      if (X_Runtime.isDebug())
        X_Log.warn("IO Error", e);
    }

  }

  private String uriBase() {
    String port = X_Properties.getProperty(X_Namespace.PROPERTY_SERVER_PORT, "");
    return System.getProperty(X_Namespace.PROPERTY_SERVER_HOST, "0.0.0.0")
      + (port.length()==0?"":":"+port);
  }

  private String drainInput
    ( InputStream in, IOCallback<IOMessage<String>> callback)
    throws IOException {
    try {
      final String message;
      final BufferedReader read = new BufferedReader(new InputStreamReader(in));
      final StringReader messageReader = new StringReader();
      if (callback instanceof LineReader) {
        messageReader.forwardTo((LineReader)callback);
      }
      String line;
      messageReader.onStart();
      while ((line = read.readLine())!=null) {
        messageReader.onLine(line);
      }
      // grab body >before< calling onEnd, as it cleans up its memory
      message = messageReader.toString();
      messageReader.onEnd();
      // All done.
      return message;
    } finally {
      in.close();
    }
  }

  /**
   * Allow subclasses to apply any settings to the connection before we open it
   * @param connect - The url connection, just before it is opened.
   * @param modifiers - The type of connection,
   * see {@link IOConstants#METHOD_GET}, {@link IOConstants#METHOD_PUT} and friends
   */
  protected void applySettings(URLConnection connect, int modifiers) {

  }

  protected String normalize(String uri) {
    if (uri.charAt(0) == '/') {
      uri = uriBase();
    }
    if (!uri.contains("://"))
      uri = "http://"+uri;
    return uri;
  }

  protected String toUrl(Class<?> classLit, String id) {
    return id;
  }

  @Override
  public void post(String url, String body, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback) {

  }

  @Override
  public void delete(String url, StringDictionary<String> headers, IOCallback<IOMessage<String>> callback) {

  }

}
