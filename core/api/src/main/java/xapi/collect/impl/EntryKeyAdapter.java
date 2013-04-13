package xapi.collect.impl;

import java.util.Iterator;
import java.util.Map.Entry;

public class EntryKeyAdapter <K, V> implements Iterable<K> {

  private class KeyIterator implements Iterator<K>{
    private Iterator<Entry<K,V>> source;
    public KeyIterator(Iterator<Entry<K, V>> source) {
      this.source = source;
    }
    @Override
    public boolean hasNext() {
      return source.hasNext();
    }
    @Override
    public K next() {
      return source.next().getKey();
    }
    @Override
    public void remove() {
      source.remove();
    }
  }

  private Iterable<Entry<K,V>> source;

  public EntryKeyAdapter(Iterable<Entry<K,V>> source) {
    this.source = source;
  }

  @Override
  public Iterator<K> iterator() {
    return new KeyIterator(source.iterator());
  }
}