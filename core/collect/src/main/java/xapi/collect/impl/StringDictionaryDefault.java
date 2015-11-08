package xapi.collect.impl;

import xapi.collect.api.StringDictionary;
import xapi.util.api.ReceivesValue;

public class StringDictionaryDefault <V> extends StringToAbstract<V> implements StringDictionary<V>{

  private static final long serialVersionUID = 7852257257033178551L;

  public StringDictionaryDefault() {
    super(Class.class.cast(Object.class)); // a filthy lie... :-/
  }

  public StringDictionaryDefault(Class<V> cls) {super(cls);}

  @Override
  public boolean hasKey(final String key) {
    return containsKey(key);
  }

  @Override
  public V getValue(final String key) {
    return get(key);
  }

  @Override
  public V setValue(final String key, final V value) {
    return put(key, value);
  }

  @Override
  public V removeValue(final String key) {
    return remove(key);
  }

  @Override
  public void clearValues() {
    clear();
  }

  @Override
  public void forKeys(final ReceivesValue<String> receiver) {
    for (final String key : keyArray()) {
      receiver.set(key);
    }
  }

}
