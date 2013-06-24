package xapi.util.impl;

import xapi.util.api.Pair;


public class PairBuilder {

  public static <X, Y> Pair<X, Y> newPair() {
    return new AbstractPair<X,Y>();
  }

  public static <X, Y> Pair<X, Y> pairOf(X x, Y y) {
    return new AbstractPair<X,Y>(x, y);
  }
  
}