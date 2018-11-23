package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.HashSet;

/**
 * A LazySet, backed by a {@link HashSet}.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazySet <T> extends Lazy<HashSet<T>> {

  public LazySet() {
    super(HashSet::new);
  }
}
