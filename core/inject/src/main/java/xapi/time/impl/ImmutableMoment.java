package xapi.time.impl;

import xapi.time.X_Time;
import xapi.time.api.Moment;

public class ImmutableMoment implements Moment{
  private static final long serialVersionUID = -5493139144266455063L;
  private final double millis;
  public ImmutableMoment(double millis) {
    this.millis = millis;
  }
  @Override
  public double millis() {
    return millis;
  }
  @Override
  public int compareTo(Moment o) {
    double delta = millis-o.millis();
    if (delta == 0)
      return 0;
    if (delta < 1 && delta > -1) {
      int bits = 0;
      double diff = Math.signum(delta);
      delta *= diff;
      while (delta < 1) {
        delta *= 2;
        bits += diff;
      }
      return (int)(diff * bits);
    }
    return (int)delta;
  }

  @Override
  public int hashCode() {
    double delta = X_Time.birth()-millis;
    return (int)(delta < 1 ? 1000000000.0*(delta) : delta);
  }

  @Override
  public boolean equals(Object obj) {
    return
      obj == this ? true
      : (obj instanceof Moment) ?
       0 == compareTo((Moment)obj) : false
     ;
  }
}