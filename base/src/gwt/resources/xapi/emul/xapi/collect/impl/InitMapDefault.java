package xapi.collect.impl;

import xapi.collect.init.AbstractInitMap;
import xapi.fu.In1Out1;
import xapi.gwt.collect.JsDictionary;
import xapi.util.api.ReceivesValue;

import java.util.Map.Entry;
import java.util.Iterator;

public class InitMapDefault <Key, Value> extends AbstractInitMap<Key,Value> {

  protected final In1Out1<Key,Value> valueProvider;

  private final JsDictionary<Value> map =
    JsDictionary.create(null);// we send null and promise not to use value[]

  public InitMapDefault(In1Out1<Key,String> keyProvider, In1Out1<Key,Value> valueProvider) {
    super(keyProvider);
    assert valueProvider != null : "Cannot use null value provider for init map.";
    this.valueProvider = valueProvider;
  }

  public static <Key, Value> xapi.collect.init.InitMapDefault<Key,Value> createInitMap(
    In1Out1<Key,String> keyProvider, In1Out1<Key,Value> valueProvider) {
    return new xapi.collect.init.InitMapDefault<Key,Value>(keyProvider, valueProvider);
  }

  @Override
  public boolean hasKey(String key) {
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
    return valueProvider.io(k);
  }


  @Override
  public void forKeys(ReceivesValue<String> receiver) {
     for (String key : map.keys())
       receiver.set(key);
  }

  public Iterable<String> keys() {
    return map.keys();
  }

  public Iterator<Entry<String, Value>> iterator() {
    return map.entries().iterator();
  }
}
