package xapi.util.api;


public interface Triple<X,Y,Z> extends Pair<X,Y> {
  Z get2();
  void set2(Z z);
}
