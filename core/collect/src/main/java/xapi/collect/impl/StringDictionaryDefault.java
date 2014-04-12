package xapi.collect.impl;

import xapi.collect.api.StringDictionary;
import xapi.util.api.ReceivesValue;

public class StringDictionaryDefault <V> extends StringToAbstract<V> implements StringDictionary<V>{

  @Override
  public boolean hasKey(String key) {
    return containsKey(key);
  }

  @Override
  public V getValue(String key) {
    return get(key);
  }

  @Override
  public V setValue(String key, V value) {
    return put(key, value);
  }

  @Override
  public V removeValue(String key) {
    return remove(key);
  }

  @Override
  public void clearValues() {
    clear();
  }

  @Override
  public void forKeys(ReceivesValue<String> receiver) {
    for (String key : keyArray()) {
      receiver.set(key);
    }
  }

}
