package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.LinkedHashSet;
/**
 * LazyLinkedSet, backed by a {@link LinkedHashSet}.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazyLinkedSet <T> extends Lazy<LinkedHashSet<T>> {

  public LazyLinkedSet() {
    super(LinkedHashSet::new);
  }
}
