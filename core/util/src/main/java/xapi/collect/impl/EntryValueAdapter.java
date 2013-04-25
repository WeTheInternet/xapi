package xapi.collect.impl;

import java.util.Iterator;
import java.util.Map.Entry;

public class EntryValueAdapter <K, V> implements Iterable<V> {

  private class ValueIterator implements Iterator<V>{
    private Iterator<Entry<K,V>> source;
    public ValueIterator(Iterator<Entry<K, V>> source) {
      this.source = source;
    }
    @Override
    public boolean hasNext() {
      return source.hasNext();
    }
    @Override
    public V next() {
      return source.next().getValue();
    }
    @Override
    public void remove() {
      source.remove();
    }
  }

  private Iterable<Entry<K, V>> source;

  public EntryValueAdapter(Iterable<Entry<K,V>> source) {
    this.source = source;
  }

  @Override
  public Iterator<V> iterator() {
    return new ValueIterator(source.iterator());
  }
}