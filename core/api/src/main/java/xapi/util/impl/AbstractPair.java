package xapi.util.impl;

import java.util.Map.Entry;

import xapi.util.X_Util;
import xapi.util.api.Pair;

public class AbstractPair <X,Y>
implements Pair<X, Y>, Entry<X,Y>
{

  public AbstractPair() {
  }
  public AbstractPair(X x,Y y) {
    set0(x);
    set1(y);
  }
  private X x;
  private Y y;
  @Override
  public X get0() {
    return x;
  }

  @Override
  public Y get1() {
    return y;
  }

  @Override
  public void set0(X x) {
    this.x=x;
  }
  @Override
  public void set1(Y y) {
    this.y=y;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)return true;
    if (obj instanceof Pair){
      Pair<?, ?> that = (Pair<?, ?>) obj;
      if (X_Util.equal(x, that.get0())){
        return X_Util.equal(y, that.get1());
      }
    }
    if (obj instanceof Entry){
      Entry<?, ?> that = (Entry<?, ?>) obj;
      if (X_Util.equal(x, that.getKey())){
        return X_Util.equal(y, that.getValue());
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash;
    if (x == null)
      hash = 0;
    else{
      hash = 37 * x.hashCode();
    }
    return hash + (y == null ? 0 : y.hashCode());
  }

  @Override
  public String toString() {
    return "["+get0()+" | "+get1()+"]";
  }
  @Override
  public X getKey() {
    return get0();
  }
  @Override
  public Y getValue() {
    return get1();
  }
  @Override
  public Y setValue(Y value) {
    try {
      return get1();
    } finally {
      set1(value);
    }
  }

}