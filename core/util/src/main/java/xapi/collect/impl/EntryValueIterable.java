package xapi.collect.impl;

import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.iterate.SizedIterator;

import java.util.Iterator;
import java.util.Map.Entry;

public class EntryValueIterable<K, V> implements SizedIterable<V> {

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
  private final Out1<Integer> size;

  public EntryValueIterable(Iterable<Entry<K,V>> source, Out1<Integer> size) {
    this.source = source;
    this.size = size;
  }

  @Override
  public SizedIterator<V> iterator() {
    return SizedIterator.of(source.iterator(), size, Entry::getValue);
  }

  @Override
  public int size() {
    return size.out1();
  }
}
