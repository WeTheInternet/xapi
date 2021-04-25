package xapi.collect.proxy.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import xapi.collect.api.CollectionOptions;

public class ListOf <V> implements List<V>{

  public ListOf() {
    this(CollectionOptions.asMutableList().build());
  }
  public ListOf(CollectionOptions opts) {

  }



  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Override
  public Iterator<V> iterator() {
    return null;
  }

  @Override
  public Object[] toArray() {
    return null;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return null;
  }

  @Override
  public boolean add(V e) {
    return false;
  }

  @Override
  public boolean remove(Object o) {
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends V> c) {
    return false;
  }

  @Override
  public boolean addAll(int index, Collection<? extends V> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public void clear() {

  }

  @Override
  public V get(int index) {
    return null;
  }

  @Override
  public V set(int index, V element) {
    return null;
  }

  @Override
  public void add(int index, V element) {

  }

  @Override
  public V remove(int index) {
    return null;
  }

  @Override
  public int indexOf(Object o) {
    return 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    return 0;
  }

  @Override
  public ListIterator<V> listIterator() {
    return null;
  }

  @Override
  public ListIterator<V> listIterator(int index) {
    return null;
  }

  @Override
  public List<V> subList(int fromIndex, int toIndex) {
    return null;
  }
  public static <V> List<V> create(CollectionOptions options) {
    return new ListOf<V>(options);
  }

}
