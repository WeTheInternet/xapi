package xapi.io.api;

import xapi.io.service.IOService;
import xapi.util.api.ConvertsValue;
import xapi.util.api.SuccessHandler;

public interface IORequestBuilder<V> {

  IORequest<V> send(SuccessHandler<V> onSuccess);

  IOService service();

  IORequestBuilder<V> setDeserializer(ConvertsValue<String,V> deserializer);

  IORequestBuilder<V> setHeader(String header, String value);

  IORequestBuilder<V> setModifier(int modifier);

  IORequestBuilder<V> setSerializer(ConvertsValue<V, String> serializer);

  IORequestBuilder<V> setValue(V value);

  IORequestBuilder<V> setUrl(String url);

}
