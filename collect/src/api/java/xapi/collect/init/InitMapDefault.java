package xapi.collect.init;

import xapi.fu.In1Out1;
import xapi.util.api.ReceivesValue;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InitMapDefault <Key, Value> extends AbstractInitMap<Key,Value>{

  protected final In1Out1<Key,Value> valueProvider;

  private final Map<String,Value> map;

  public InitMapDefault(In1Out1<Key,String> keyProvider, In1Out1<Key,Value> valueProvider) {
    this(keyProvider, valueProvider, new ConcurrentHashMap<>());
  }
  public InitMapDefault(In1Out1<Key,String> keyProvider, In1Out1<Key,Value> valueProvider, Map<String, Value> backingMap) {
    super(keyProvider);
    this.map = backingMap instanceof ConcurrentMap ? backingMap : Collections.synchronizedMap(backingMap);
    assert valueProvider != null : "Cannot use null value provider for init map.";
    this.valueProvider = valueProvider;
  }


  public static <Key, Value> InitMapDefault<Key,Value> createInitMap(
    In1Out1<Key,String> keyProvider, In1Out1<Key,Value> valueProvider) {
    return new InitMapDefault<>(keyProvider, valueProvider);
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
    return map.remove(key);
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
     for (String key : map.keySet())
       receiver.set(key);
  }

  public int size() {
    return map.size();
  }

  public Iterable<String> keys() {
    return map.keySet();
  }

  @Override
  public Iterator<Entry<String, Value>> iterator() {
    return map.entrySet().iterator();
  }

}
