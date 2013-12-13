package xapi.collect.impl;

import java.util.ArrayList;

import xapi.inject.impl.SingletonProvider;

/**
 * LazyList is backed by an ArrayList<>, created on demand.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <T>
 */
public class LazyList <T> extends SingletonProvider<ArrayList<T>>{
  @Override
  protected ArrayList<T> initialValue() {
    return new ArrayList<T>();
  }

}
