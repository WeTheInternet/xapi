package xapi.collect.impl;

import java.util.HashSet;

import xapi.inject.impl.SingletonProvider;

/**
 * A LazySet, backed by a {@link HashSet}.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazySet <T> extends SingletonProvider<HashSet<T>>{
  @Override
  protected HashSet<T> initialValue() {
    return new HashSet<T>();
  }
}