package xapi.fu.java;

import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.iterate.SizedIterator;

import java.util.Map.Entry;

public class EntryKeyIterable<K, V> implements SizedIterable<K> {

  private final Out1<Integer> size;

  private Iterable<Entry<K,V>> source;

  public EntryKeyIterable(Iterable<Entry<K,V>> source, Out1<Integer> size) {
    this.source = source;
    this.size = size;
  }

  @Override
  public SizedIterator<K> iterator() {
    return SizedIterator.of(source.iterator(), size, Entry::getKey);
  }

  @Override
  public int size() {
    return size.out1();
  }
}
