package xapi.collect.impl;

import xapi.fu.Lazy;

import java.util.ArrayList;

/**
 * LazyList is backed by an ArrayList<>, created on demand.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazyList <T> extends Lazy<ArrayList<T>> {

  public LazyList() {
    super(ArrayList::new);
  }

}
