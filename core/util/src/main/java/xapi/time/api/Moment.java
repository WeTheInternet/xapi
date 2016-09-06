package xapi.time.api;

import xapi.fu.IsImmutable;

import java.io.Serializable;

public interface Moment extends Serializable, Comparable <Moment> {

  /**
   * Warning: You have to handle NAN in comparisons of millis,
   * as that is the value for NULL;
   * this allows MIN_VALUE and -1 to not be mistaken for "null".
   */
  Moment NULL = (Moment & IsImmutable)()->Double.NaN;
  Moment ZERO = (Moment & IsImmutable)()->0;
  Moment ONE = (Moment & IsImmutable)()->1;
  Moment NEG_ONE = (Moment & IsImmutable)()->-1;
  Moment TWO = (Moment & IsImmutable)()->2;

  double millis();

  @Override
  default int compareTo(final Moment o) {
    final double yours = o.millis();
    final double mine = millis();
    if (Double.isNaN(yours)) {
      // "you" are NAN; move you to the back
      // TODO: warn NAN operation
      return -1;
    }
    if (Double.isNaN(mine)) {
      // "I" am NAN; move you to the front
      // TODO: warn NAN operation
      return 1;
    }
    final double diff = mine - yours;
    return diff == 0. ? 0
        : diff > 0.
        ? diff > (double)Integer.MAX_VALUE
        ? Integer.MAX_VALUE
        : Math.min(1, (int)diff)
        : diff < (double)((Integer.MIN_VALUE+1))
        // clamp to MIN_VALUE + 1, in case you need to consider MIN_VALUE specially
        ? Integer.MIN_VALUE + 1
        : Math.min(-1, (int) diff);
  }

//
//  long me =
///*js:
//function DoubleToIEEE(f)
//{
//  var buf = new ArrayBuffer(8);
//  (new Float64Array(buf))[0] = f;
//  // We will also process these bits as ints to avoid long emulation.
//  // Thus, we do not bother with a doubleToLongBits method, as long emulation sucks
//  return [ (new Uint32Array(buf))[0] ,(new Uint32Array(buf))[1] ];
//}(java: millis() :java)
//:js*/
///*java: Double.doubleToRawLongBits(millis()) :java*/
//      Double.doubleToLongBits(millis());
//
//  long you =
///*js:
//function DoubleToIEEE(f)
//{
//  var buf = new ArrayBuffer(8);
//  (new Float64Array(buf))[0] = f;
//  // We will also process these bits as ints to avoid long emulation.
//  // Thus, we do not bother with a doubleToLongBits method, as long emulation sucks
//  return [ (new Uint32Array(buf))[0] ,(new Uint32Array(buf))[1] ];
//}(java: o.millis() :java)
//:js*/
///*java: Double.doubleToRawLongBits(o.millis()) :java*/
//      Double.doubleToLongBits(o.millis());
//    return Long.compare(me, you);


  default Moment plus(double millis) {
    double me = millis();
    double later = me + millis;
    return (IsImmutable & Moment)()->later;
  }

  default Moment minus(double millis) {
    double me = millis();
    double later = me - millis;
    return (IsImmutable & Moment)()->later;
  }
}
