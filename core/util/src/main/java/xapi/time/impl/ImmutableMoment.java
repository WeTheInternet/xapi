package xapi.time.impl;

import xapi.time.api.Moment;

public class ImmutableMoment implements Moment{
  private static final long serialVersionUID = -5493139144266455063L;
  private final double millis;
  private int hash;
  public ImmutableMoment(double millis) {
    this.millis = millis;
  }
  @Override
  public double millis() {
    return millis;
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = Double.hashCode(millis());
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    return
      obj == this ||
      ( (obj instanceof Moment) &&  0 == compareTo((Moment)obj) );
  }
}
