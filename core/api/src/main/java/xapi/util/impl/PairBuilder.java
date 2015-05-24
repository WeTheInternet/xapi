package xapi.util.impl;

import java.util.Map.Entry;

import xapi.util.api.Pair;


public class PairBuilder {

  public static <X, Y> Pair<X, Y> newPair() {
    return new AbstractPair<X,Y>();
  }

  public static <X, Y> Pair<X, Y> pairOf(final X x, final Y y) {
    return new AbstractPair<X,Y>(x, y);
  }

  public static <X, Y> Entry<X, Y> entryOf(final X x, final Y y) {
    return new AbstractPair<X,Y>(x, y);
  }

}