package xapi.collect.impl;

import xapi.gwt.collect.JsDictionary;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;

public class InitMapDefault <Key, Value> extends AbstractInitMap<Key,Value>{

  protected final ConvertsValue<Key,Value> valueProvider;

  private final JsDictionary<Value> map = 
    JsDictionary.create(null);// we send null and promise not to use value[]

  public InitMapDefault(ConvertsValue<Key,String> keyProvider, ConvertsValue<Key,Value> valueProvider) {
    super(keyProvider);
    assert valueProvider != null : "Cannot use null value provider for init map.";
    this.valueProvider = valueProvider;
  }

  public static <Key, Value> InitMapDefault<Key,Value> createInitMap(
    ConvertsValue<Key,String> keyProvider, ConvertsValue<Key,Value> valueProvider) {
    return new InitMapDefault<Key,Value>(keyProvider, valueProvider);
  }

  @Override
  public boolean hasValue(String key) {
    return map.containsKey(key);
  }

  @Override
  public Value getValue(String key) {
    return map.get(key);
  }

  @Override
  public Value setValue(String key, Value value) {
    return map.put(key, value);
  }

  @Override
  public Value removeValue(String key) {
    return map.removeAndReturn(key);
  }

  @Override
  public void clearValues() {
    map.clear();
  }

  @Override
  public Value initialize(Key k) {
    return valueProvider.convert(k);
  }


  @Override
  public void forKeys(ReceivesValue<String> receiver) {
     for (String key : map.keys())
       receiver.set(key);
  }
}
