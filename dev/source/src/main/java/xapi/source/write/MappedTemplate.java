package xapi.source.write;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MappedTemplate extends Template {

  private Map<String, Integer> positions;

  public MappedTemplate(String template, Iterable<String> items) {
    this(template, StringerMatcher.DEFAULT_TO_STRINGER, toArray(items));
  }

  public MappedTemplate(String template, String ... replaceables) {
    this(template, StringerMatcher.DEFAULT_TO_STRINGER, replaceables);
  }

  public MappedTemplate(String template, StringerMatcher matcher, Iterable<String> items) {
    this(template, matcher, toArray(items));
  }

  public MappedTemplate(String template, StringerMatcher matcher, String ... replaceables) {
    super(template, matcher, replaceables);
    positions = new HashMap<String, Integer>();
    for (int i = 0, m = replaceables.length; i < m; i++) {
      positions.put(replaceables[i], i);
    }
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
    return object;
  }

  protected void warnMissing(String key) {
    // Default does nothing; this is here so you can implement logging as you see fit.
  }

  public boolean hasKey(String key) {
    return positions.containsKey(key);
  }

}
