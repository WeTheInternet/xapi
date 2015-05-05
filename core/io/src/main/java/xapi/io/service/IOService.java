package xapi.io.service;

import xapi.collect.api.StringDictionary;
import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.IORequest;
import xapi.io.api.IORequestBuilder;
import xapi.util.api.ConvertsValue;
import xapi.util.api.RemovalHandler;

public interface IOService {

  <V> IORequestBuilder<V> request(Class<V> classLit, String id);

  /**
   * Registers a de/serializer for a given class literal,
   * and returns a removal method to undo the registration.
   *
   * The ONLY safe way to use the undo method is to
   * a) synchronize(classLit) {
   * b) RemovalHandler r = registerParser(...);
   * try {
   *  // do stuff, but don't try to acquire a lock.
   *  // just send your request and get out.
   *  } finally {
   *  r.remove();// resets the given de/serializer combo.
   *  }
   *
   * Do not keep references of the removal handler beyond the method that
   * called registerParser, unless you are willing to risk indeterminism.
   * We will do check-and-set,
   *
   * Also, for code to work in gwt or other transpiled targets,
   * you MUST be using a class literal, and not a class _REFERENCE_.
   *
   * registerParser(IPojo.class, ...)  - Good!
   *
   * Class theClass = IPojo.class;
   * registerParser(theClass, ...) - Bad!
   *
   * When you use class literals, the reference can be reliably resolved,
   * otherwise it's on you to record every method passing that class lit along,
   * and do something filthy, like stash it in a ThreadLocal&lt;Stack&gt;,.
   *
   * The deal for magically generated code is "pass in the interface you want,
   * get back the best (or only) implementation for your platform.
   *
   * @param classLit - The type to register serializers for.
   * @param serializer - ConvertsValue<V, String>
   * @param deserializer - ConvertsValue<String, V>
   * @return - A removal method to undo registration;
   * using this in a try / finally block, in case you just
   * want to borrow the de/serialization process.
   */
  <V> RemovalHandler registerParser(Class<V> classLit,
    ConvertsValue<V, String> serializer,
    ConvertsValue<String,V> deserializer);

  void put(String url, byte[] body, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback);

  IORequest<String> get(String url, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback);

  IORequest<String> post(String url, String body, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback);

  void delete(String url, StringDictionary<String> headers,
    IOCallback<IOMessage<String>> callback);

}
