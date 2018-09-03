package xapi.fu;

import xapi.annotation.compile.Generated;

@Generated(date="2016-10-12T02:23.192-08:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/In.xapi", "67kn60nk6owhihql"})
public interface In2 <I1, I2> extends HasInput, Lambda, Rethrowable{

  public abstract void in2 (I1 in1, I2 in2) ;

  public default In2<I2, I2> map1 (In1Out1<To, I1> mapper) {
    return (i1, i2) -> in(mapper.io(i1), i2);
  }

  public default In1 merge1And2 (In1Out1<T, I1> map1, In1Out1<T, I2> map2) {
    return (t) -> in(map1.io(i1), map2.io(i2));
  }

  public default In1<I$v, I$v> provide1 (I1 val) {
    return (, i2) -> in(val, i2);
  }

  public default In1<I$v, I$v> provide1Deferred (Out1<I1> val) {
    return (, i2) -> in(val.out1(), i2);
  }

  public default In1<I2, I2> provide1Immediate (Out1<I1> val) {
    I1 result = val.out1();
    return (, i2) -> in(result, i2);
  }

  public default In3<I1, I1, I1> require1After (In1<T> require) {
    return (t, i1, i2) -> {
        in(, i1, i2);
        require.in(t);
    };
  }

  public default In3<I1, I1, I1> require1Before (In1<T> require) {
    return (t, i1, i2) -> {
        require.in(t);
        in(, i1, i2);
    };
  }

  public default In2<I2, I2> spy1Before (In1<I1> spy) {
    return (i1, i2) -> {
      spy.in1(i1);
      in(i1, i2);
    };
  }

  public default In2<I2, I2> spy1After (In1<I1> spy) {
    return (i1, i2) -> {
      in(i1, i2);
      spy.in1(i1);
    };
  }

  public default In2<I1, I1> swap1And2 () {
    return (i2, i1) -> in(i1, i2);
  }

  public default In1<I2, I2> use1For2 (In1Out1<I1, I2> val) {
    return (i1) -> in(i1, mapper.io(i1));
  }

  public default In2<To, To> map2 (In1Out1<To, I2> mapper) {
    return (i1, i2) -> in(i1, mapper.io(i2));
  }

  public default In1<I$v, I$v> provide2 (I2 val) {
    return (i1) -> in(i1, val);
  }

  public default In1<I$v, I$v> provide2Deferred (Out1<I2> val) {
    return (i1) -> in(i1, val.out1());
  }

  public default In1 provide2Immediate (Out1<I2> val) {
    I2 result = val.out1();
    return (i1) -> in(i1, result);
  }

  public default In3<T, T, T> require2After (In1<T> require) {
    return (i1, t, i2) -> {
        in(i1, i2);
        require.in(t);
    };
  }

  public default In3<T, T, T> require2Before (In1<T> require) {
    return (i1, t, i2) -> {
        require.in(t);
        in(i1, i2);
    };
  }

  public default In2<I2, I2> spy2Before (In1<I2> spy) {
    return (i1, i2) -> {
      spy.in1(i2);
      in(i1, i2);
    };
  }

  public default In2<I2, I2> spy2After (In1<I2> spy) {
    return (i1, i2) -> {
      in(i1, i2);
      spy.in1(i2);
    };
  }

  public default In1 use2For1 (In1Out1<I2, I1> val) {
    return (, i2) -> in(mapper.io(i2), i2);
  }

}
