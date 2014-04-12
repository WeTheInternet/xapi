package xapi.source.write;

import java.util.HashMap;
import java.util.Map;

public class MappedTemplate extends Template {

  private Map<String, Integer> positions;
  
  public MappedTemplate(String template, Iterable<String> items) {
    this(template, toArray(items));
  }
  
  public MappedTemplate(String template, String ... replaceables) {
    super(template, replaceables);
    positions = new HashMap<String, Integer>();
    for (int i = 0, m = replaceables.length; i < m; i++) {
      positions.put(replaceables[i], i);
    }
  }
  
  public String applyMap(Map<String, Object> map) {
    Object[] values = new Object[positions.size()];
    for (String key : map.keySet()) {
      Integer pos = positions.get(key);
      if (pos == null) {
        warnMissing(key);
      } else {
        values[pos] = map.get(key);
      }
    }
    return apply(values);
  }

  protected void warnMissing(String key) {
    // Default does nothing; this is here so you can implement logging as you see fit.
  }

  public boolean hasKey(String key) {
    return positions.containsKey(key);
  }
  
}
