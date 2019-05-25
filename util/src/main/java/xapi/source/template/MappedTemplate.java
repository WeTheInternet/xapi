package xapi.source.template;

import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.log.Log;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.source.write.ToStringer;
import xapi.source.write.Template;

import java.util.Map.Entry;

public class MappedTemplate extends Template {

  private final MapLike<String, Integer> positions;
  private final MapLike<String, In2Out1<String, Object, Object>> factories;

  public MappedTemplate(String template, Iterable<String> items) {
    this(template, ToStringer.DEFAULT_TO_STRINGER, toArray(items));
  }

  public MappedTemplate(String template, String ... replaceables) {
    this(template, ToStringer.DEFAULT_TO_STRINGER, replaceables);
  }

  public MappedTemplate(String template, ToStringer matcher, Iterable<String> items) {
    this(template, matcher, toArray(items));
  }

  public MappedTemplate(String template, ToStringer matcher, String ... replaceables) {
    super(template, matcher, replaceables);
    positions = X_Jdk.mapOrderedInsertion();
    factories = X_Jdk.mapOrderedInsertion();
    for (int i = 0, m = replaceables.length; i < m; i++) {
      positions.put(replaceables[i], i);
    }
  }

  public MappedTemplate addKeyMapper(String key, In2Out1<String, Object, Object> mapper) {
    factories.put(key, mapper);
    return this;
  }

  public String applyMap(Iterable<Entry<String, Object>> map) {
    Object[] values = new Object[positions.size()];
    for (Entry<String, Object> entry : map) {
      String key = entry.getKey();
      Integer pos = positions.get(key);
      if (pos == null) {
        warnMissing(key);
      } else {
        values[pos] = retrieve(key, entry.getValue());
      }
    }
    return apply(values);
  }

  /**
   * Allow subclasses to perform additional data retrieval logic.
   */
  protected Object retrieve(String key, Object object) {
    final In2Out1<String, Object, Object> factory = factories.get(key);
    if (factory != null) {
      return factory.io(key, object);
    }
    if (object instanceof In1Out1) {
      final Object result = ((In1Out1) object).io(key);
      return result == null ? key : result;
    }
    return object;
  }

  protected void warnMissing(String key) {
    // Default will log if the current instance implement Log
    if (this instanceof Log) {
      ((Log)this).log(MappedTemplate.class, "No template result for key [" + key + "]");
    }
  }

  public boolean hasKey(String key) {
    return positions.has(key);
  }

}
