package xapi.collect.impl;

import java.util.LinkedHashSet;

import xapi.inject.impl.SingletonProvider;
/**
 * LazyLinkedSet, backed by a {@link LinkedHashSet}. 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazyLinkedSet <T> extends SingletonProvider<LinkedHashSet<T>>{
  @Override
  protected LinkedHashSet<T> initialValue() {
    return new LinkedHashSet<T>();
  }
}