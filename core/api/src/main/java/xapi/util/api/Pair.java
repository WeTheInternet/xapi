package xapi.util.api;

import xapi.util.impl.PairBuilder;

import static xapi.util.X_Util.arrayRegex;

public interface Pair <X,Y>{

  static Pair<String, Integer> extractArrayDepth(String from) {
    int arrayDepth = 0;

      while (from.matches(".*"+ arrayRegex)) {
      arrayDepth ++;
      from = from.replaceFirst(arrayRegex, "");
    }
    return PairBuilder.pairOf(from, arrayDepth);
  }

  X get0();
  Y get1();
  void set0(X x);
  void set1(Y x);
}
