package xapi.util.api;

import xapi.util.X_Util;
import xapi.util.impl.PairBuilder;

public interface Pair <X,Y>{

  static Pair<String, Integer> extractArrayDepth(String from) {
    int arrayDepth = 0;

      while (from.matches(".*"+ X_Util.arrayRegex)) {
      arrayDepth ++;
      from = from.replaceFirst(X_Util.arrayRegex, "");
    }
    return PairBuilder.pairOf(from, arrayDepth);
  }

  X get0();
  Y get1();
  void set0(X x);
  void set1(Y x);
}
