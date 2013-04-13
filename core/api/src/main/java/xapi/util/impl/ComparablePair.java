package xapi.util.impl;

import xapi.util.api.Pair;

public class ComparablePair <X extends Comparable<X>,Y extends Comparable<Y>> implements Pair<X, Y>,Comparable<ComparablePair<X, Y>>{

  private X x;
  private Y y;
  
  
  public ComparablePair() {
  }
  public ComparablePair(X x,Y y) {
    set0(x);
    set1(y);
  }
  
  private boolean xFirst = true;
    @Override
    public int compareTo(ComparablePair<X, Y> o) {
      int ret = 0;
      if (isXFirst()){
        try{
          ret = get0().compareTo(o.get0());
          if (ret==0){
            try{
              ret = get1().compareTo(o.get1());
            }catch (NullPointerException e) {
              //null gets sent backward
              return get1()==null?-1:1;
            }
          }
        }catch (NullPointerException e) {
          return get0()==null?-1:1;
        }
      }else{
        try{
          ret = get1().compareTo(o.get1());
          if (ret==0){//passed first compare
            try{
              ret = get0().compareTo(o.get0());
            }catch (NullPointerException e) {
              //null gets sent backward
              return get0()==null?-1:1;
            }
          }
        }catch (NullPointerException e) {
          return get1()==null?-1:1;
        }
      }
      return ret;
    }

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

    public boolean isXFirst() {
      return xFirst;
    }

    public void setXFirst(boolean xFirst) {
      this.xFirst = xFirst;
    }
}
