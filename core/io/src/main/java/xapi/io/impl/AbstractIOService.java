package xapi.io.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo.Many;
import xapi.fu.Out1;
import xapi.io.IOConstants;
import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.IORequest;
import xapi.io.api.IORequestBuilder;
import xapi.io.service.IOService;
import xapi.log.api.LogLevel;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;
import xapi.util.api.RemovalHandler;

import static xapi.collect.X_Collect.newClassMap;

public abstract class AbstractIOService <Transport> implements IOService{

  public AbstractIOService() {
  }

  protected static final IORequest<String> cancelled = new IORequest<String>() {

    @Override
    public boolean isPending() {
      return false;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public void cancel() {
    }

    @Override
    public String response() {
      return null;
    }

    @Override
    public Many<String> headers() {
      return X_Collect.newStringMultiMap(String.class);
    }

    @Override
    public int getStatusCode() {
      return STATUS_CANCELLED;
    }

    @Override
    public String getStatusText() {
      return "Request Cancelled";
    }
  };

  public abstract class AbstractIORequest implements IORequest<String> {

    private boolean pending = true;
    private String value;
    private volatile boolean cancel;
    private boolean started;
    protected Out1<Many<String>> resultHeaders;
    private int statusCode = STATUS_INCOMPLETE;
    private String statusText = "Request Incomplete";

    @Override
    public boolean isPending() {
      return pending;
    }

    public void start() {
      this.started = true;
    }

    @Override
    public boolean isSuccess() {
      return pending == false && cancel == false && value != null;
    }

    @Override
    public void cancel() {
      this.pending = false;
      this.cancel = false;
    }

    @Override
    public Many<String> headers() {
      return resultHeaders == null ? X_Collect.newStringMultiMap(String.class) : resultHeaders.out1();
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isCancelled() {
      return cancel;
    }

    /**
     * @return -> value
     */
    public String getValue() {
      return value;
    }

    /**
     * @param value -> set value
     */
    public void setValue(final String value) {
      pending = false;
      this.value = value;
    }

    public void setResultHeaders(final Out1<Many<String>> resultHeaders) {
      this.resultHeaders = resultHeaders;
    }

    public void setStatus(final int statusCode, final String statusText) {
      this.statusCode = statusCode;
      this.statusText = statusText;
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getStatusText() {
      return statusText;
    }

  }

  @SuppressWarnings("unchecked")
  private static final Class<ConvertsValue<?,String>> SERIAL_CLASS
    = Class.class.cast(ConvertsValue.class);

  @SuppressWarnings("unchecked")
  private static final Class<ConvertsValue<String, ?>> DESERIAL_CLASS
    = Class.class.cast(ConvertsValue.class);

  protected final ClassTo<ConvertsValue<?, String>> serializers = newClassMap(SERIAL_CLASS);

  protected final ClassTo<ConvertsValue<String, ?>> deserializers = newClassMap(DESERIAL_CLASS);

  @Override
  @SuppressWarnings("unchecked") // The class key is always checked for us...
  public <V> IORequestBuilder<V> request(final Class<V> classLit, final String id) {
    return this.<V>createRequestBuilder()
      .setUrl(toUrl(classLit, id))
      .setDeserializer(ConvertsValue.class.cast(deserializers.get(classLit)))
      .setSerializer(ConvertsValue.class.cast(serializers.get(classLit)))
    ;
  }

  protected <V> IORequestBuilderDefault<V> createRequestBuilder() {
    return new IORequestBuilderDefault<V>();
  }

  protected LogLevel logLevel() {
    return LogLevel.DEBUG;
  }

  @Override
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  public <V> RemovalHandler registerParser(final Class<V> classLit,
    final ConvertsValue<V,String> serializer,
    final ConvertsValue<String,V> deserializer
    ) {
    final ConvertsValue<?,String> currentSerializer = serializers.put(classLit, serializer);
    final ConvertsValue<String,?> currentDeserialize = deserializers.put(classLit, deserializer);
    // In case a user wants to wrap the existing serializers, we will send them the previous instances.
    if (serializer instanceof ReceivesValue) {
      ((ReceivesValue)serializer).set(currentSerializer);
    }
    if (deserializer instanceof ReceivesValue) {
      ((ReceivesValue)deserializer).set(currentDeserialize);
    }
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
  public void put(final String url, final byte[] body, final StringDictionary<String> headers,
    final IOCallback<IOMessage<String>> callback) {

  }

  protected String uriBase() {
    final String port = X_Properties.getProperty(X_Namespace.PROPERTY_SERVER_PORT, "");
    return System.getProperty(X_Namespace.PROPERTY_SERVER_HOST, "0.0.0.0")
      + (port.length()==0?"":":"+port);
  }

  /**
   * Allow subclasses to apply any settings to the connection before we open it
   * @param connect - An instance of whatever transport mechanism this IO service uses, just before it is opened.
   * @param modifiers - The type of connection,
   * see {@link IOConstants#METHOD_GET}, {@link IOConstants#METHOD_PUT} and friends
   */
  protected void applySettings(final Transport connect, final int modifiers) {

  }

  protected String normalize(String uri) {
    if (uri.charAt(0) == '/') {
      uri = uriBase() + uri;
    }
    if (!uri.contains("://")) {
      uri = "http://"+uri;
    }
    return uri;
  }

  protected String toUrl(final Class<?> classLit, final String id) {
    return id;
  }

  @Override
  public void delete(final String url, final StringDictionary<String> headers, final IOCallback<IOMessage<String>> callback) {

  }

}
