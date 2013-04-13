package xapi.collect.proxy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;

public class SetOf <V> implements Set<V>{

  private final Comparator<V> cmp;


  public SetOf() {
    this(X_Collect.MUTABLE_SET);
  }
  public SetOf(Comparator<V> cmp) {
    this.cmp = cmp;
  }
  public SetOf(CollectionOptions opts) {
    this(X_Collect.<V>getComparator(opts));
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean contains(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Iterator<V> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object[] toArray() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean add(V e) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean remove(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends V> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void clear() {
    // TODO Auto-generated method stub

  }

}
