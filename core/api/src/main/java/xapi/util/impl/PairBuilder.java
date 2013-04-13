package xapi.util.impl;

import xapi.util.api.Pair;

public class PairBuilder {
  public static <X, Y> Pair <X, Y> of(X x, Y y) {
    return new AbstractPair<X,Y>(x, y);
  }
  public static <X, Y> Pair <X, Y> newPair() {
    return new AbstractPair<X,Y>();
  }
}