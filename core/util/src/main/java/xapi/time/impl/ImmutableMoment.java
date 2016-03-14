package xapi.time.impl;

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
    long me =
/*js:
function DoubleToIEEE(f)
{
  var buf = new ArrayBuffer(8);
  (new Float64Array(buf))[0] = f;
  // We will also process these bits as ints to avoid long emulation.
  // Thus, we do not bother with a doubleToLongBits method, as long emulation sucks
  return [ (new Uint32Array(buf))[0] ,(new Uint32Array(buf))[1] ];
}(java: millis() :java)
:js*/
/*java: Double.doubleToRawLongBits(millis()) :java*/
        Double.doubleToLongBits(millis());

    long you =
/*js:
function DoubleToIEEE(f)
{
  var buf = new ArrayBuffer(8);
  (new Float64Array(buf))[0] = f;
  // We will also process these bits as ints to avoid long emulation.
  // Thus, we do not bother with a doubleToLongBits method, as long emulation sucks
  return [ (new Uint32Array(buf))[0] ,(new Uint32Array(buf))[1] ];
}(java: o.millis() :java)
:js*/
/*java: Double.doubleToRawLongBits(o.millis()) :java*/
        Double.doubleToLongBits(o.millis());
    return Long.compare(me, you);
  }

  @Override
  public int hashCode() {
    return Double.hashCode(millis());
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
