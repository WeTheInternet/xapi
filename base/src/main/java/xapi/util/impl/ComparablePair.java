package xapi.util.impl;

import xapi.util.api.Pair;
/**
 * A pair which implements comparable, using both components as comparable objects.
 * 
 * It allows you to define whether to compare x before y (slot 0 or 1), or vice versa.
 * This is useful in graphical mapping, where you may want to sort a map horizontally or vertically.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <X>
 * @param <Y>
 */
public class ComparablePair <X extends Comparable<X>,Y extends Comparable<Y>> 
implements Pair<X, Y>,Comparable<ComparablePair<X, Y>>{

  private X x;
  private Y y;
  
  
  public ComparablePair() {
  }
  public ComparablePair(X x,Y y) {
    
    set0(x);
    set1(y);
  }
  
    @Override
    public int compareTo(ComparablePair<X, Y> o) {
      X _x = o.get0();
      Y _y = o.get1();
      final int dX, dY;
      if (x == null) {
        if (_x != null) {
          return 1;
        }
        dX = 0;
      } else {
        if (_x == null) {
          return -1;
        }
        dX = x.compareTo(_x);
      }
      
      if (y == null) {
        if (_y != null) {
          return 1;
        }
        dY = 0;
      } else {
        if (_y == null) {
          return -1;
        }
        dY = y.compareTo(_y);
      }
      
      return dX ^ dY;
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
}
