package xapi.gwt.model;

import static xapi.util.impl.PairBuilder.entryOf;

import java.util.Iterator;
import java.util.Map.Entry;

import xapi.model.impl.AbstractModel;

public class ModelGwt extends AbstractModel{

  private final class Itr implements Iterable<Entry<String, Object>> {

    private final String[] keys;

    private Itr(final String[] keys) {
      this.keys = keys;
    }

    @Override
    public Iterator<Entry<String, Object>> iterator() {
      return new Iterator<Entry<String,Object>>() {

        int pos = 0;
        @Override
        public boolean hasNext() {
          return pos < keys.length;
        }

        @Override
        public Entry<String, Object> next() {
          final String key = keys[pos];
          Object value = getProperty(key);
          if (value == null) {
            final Class<?> type = getPropertyType(key);
            if (type.isPrimitive()) {
              value = AbstractModel.getPrimitiveValue(type);
            }
          }
          return entryOf(key, value);
        }
      };
    }
  }

  @Override
  public Iterable<Entry<String, Object>> getProperties() {
    // Because we know all generated Gwt modules MUST implement property names,
    // We can safely enforce serialization ordering using .getPropertyNames()
    final String[] names = getPropertyNames();
    return new Itr(names);
  }

  @Override
  public Class<?> getPropertyType(final String key) {
    throw new UnsupportedOperationException("Type "+getClass()+" does not handle property type "+key);
  }

  @Override
  public String[] getPropertyNames() {
    return new String[0];
  }
}
