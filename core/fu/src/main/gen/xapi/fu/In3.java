package xapi.fu;

import xapi.annotation.compile.Generated;

@Generated(date="2016-10-12T02:23.191-08:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/In.xapi", "ldpckmwewja9h5pt"})
public interface In3 <I1, I2, I3> extends HasInput, Lambda, Rethrowable{

  public abstract void in3 (I1 in1, I2 in2, I3 in3) ;

  public default In3<I3, I3, I3> map1 (In1Out1<To, I1> mapper) {
    return (i1, i2, i3) -> in(mapper.io(i1), i2, i3);
  }

  public default In2<I3, I3, I3, I3> merge1And2 (In1Out1<T, I1> map1, In1Out1<T, I2> map2) {
    return (t, i3) -> in(map1.io(i1), map2.io(i2), i3);
  }

  public default In2 merge1And3 (In1Out1<T, I1> map1, In1Out1<T, I3> map3) {
    return (t, i2) -> in(map1.io(i1), i2, map3.io(i3));
  }

  public default In2<I$v, I$v, I$v> provide1 (I1 val) {
    return (, i2, i3) -> in(val, i2, i3);
  }

  public default In2<I$v, I$v, I$v> provide1Deferred (Out1<I1> val) {
    return (, i2, i3) -> in(val.out1(), i2, i3);
  }

  public default In2<I3, I3, I3> provide1Immediate (Out1<I1> val) {
    I1 result = val.out1();
    return (, i2, i3) -> in(result, i2, i3);
  }

  public default In4<I2, I2, I2, I2> require1After (In1<T> require) {
    return (t, i1, i2, i3) -> {
        in(, i1, i2, i3);
        require.in(t);
    };
  }

  public default In4<I2, I2, I2, I2> require1Before (In1<T> require) {
    return (t, i1, i2, i3) -> {
        require.in(t);
        in(, i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy1Before (In1<I1> spy) {
    return (i1, i2, i3) -> {
      spy.in1(i1);
      in(i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy1After (In1<I1> spy) {
    return (i1, i2, i3) -> {
      in(i1, i2, i3);
      spy.in1(i1);
    };
  }

  public default In3<I3, I3, I3> swap1And2 () {
    return (i2, i1, i3) -> in(i1, i2, i3);
  }

  public default In2<I3, I3, I3> use1For2 (In1Out1<I1, I2> val) {
    return (i1, i3) -> in(i1, mapper.io(i1), i3);
  }

  public default In3<I1, I1, I1> swap1And3 () {
    return (i3, i2, i1) -> in(i1, i2, i3);
  }

  public default In2<I3, I3, I3> use1For3 (In1Out1<I1, I3> val) {
    return (i1, i2) -> in(i1, i2, mapper.io(i1));
  }

  public default In3<I3, I3, I3> map2 (In1Out1<To, I2> mapper) {
    return (i1, i2, i3) -> in(i1, mapper.io(i2), i3);
  }

  public default In2 merge2And3 (In1Out1<T, I2> map2, In1Out1<T, I3> map3) {
    return (t, i1) -> in(i1, map2.io(i2), map3.io(i3));
  }

  public default In2<I$v, I$v, I$v> provide2 (I2 val) {
    return (i1, i3) -> in(i1, val, i3);
  }

  public default In2<I$v, I$v, I$v> provide2Deferred (Out1<I2> val) {
    return (i1, i3) -> in(i1, val.out1(), i3);
  }

  public default In2<I3, I3, I3> provide2Immediate (Out1<I2> val) {
    I2 result = val.out1();
    return (i1, i3) -> in(i1, result, i3);
  }

  public default In4<I2, I2, I2, I2> require2After (In1<T> require) {
    return (i1, t, i2, i3) -> {
        in(i1, i2, i3);
        require.in(t);
    };
  }

  public default In4<I2, I2, I2, I2> require2Before (In1<T> require) {
    return (i1, t, i2, i3) -> {
        require.in(t);
        in(i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy2Before (In1<I2> spy) {
    return (i1, i2, i3) -> {
      spy.in1(i2);
      in(i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy2After (In1<I2> spy) {
    return (i1, i2, i3) -> {
      in(i1, i2, i3);
      spy.in1(i2);
    };
  }

  public default In2<I3, I3, I3> use2For1 (In1Out1<I2, I1> val) {
    return (, i2, i3) -> in(mapper.io(i2), i2, i3);
  }

  public default In3<I2, I2, I2> swap2And3 () {
    return (i1, i3, i2) -> in(i1, i2, i3);
  }

  public default In2<I3, I3, I3> use2For3 (In1Out1<I2, I3> val) {
    return (i1, i2) -> in(i1, i2, mapper.io(i2));
  }

  public default In3<To, To, To> map3 (In1Out1<To, I3> mapper) {
    return (i1, i2, i3) -> in(i1, i2, mapper.io(i3));
  }

  public default In2<I$v, I$v, I$v> provide3 (I3 val) {
    return (i1, i2) -> in(i1, i2, val);
  }

  public default In2<I$v, I$v, I$v> provide3Deferred (Out1<I3> val) {
    return (i1, i2) -> in(i1, i2, val.out1());
  }

  public default In2 provide3Immediate (Out1<I3> val) {
    I3 result = val.out1();
    return (i1, i2) -> in(i1, i2, result);
  }

  public default In4<T, T, T, T> require3After (In1<T> require) {
    return (i1, i2, t, i3) -> {
        in(i1, i2, i3);
        require.in(t);
    };
  }

  public default In4<T, T, T, T> require3Before (In1<T> require) {
    return (i1, i2, t, i3) -> {
        require.in(t);
        in(i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy3Before (In1<I3> spy) {
    return (i1, i2, i3) -> {
      spy.in1(i3);
      in(i1, i2, i3);
    };
  }

  public default In3<I3, I3, I3> spy3After (In1<I3> spy) {
    return (i1, i2, i3) -> {
      in(i1, i2, i3);
      spy.in1(i3);
    };
  }

  public default In2 use3For1 (In1Out1<I3, I1> val) {
    return (, i2, i3) -> in(mapper.io(i3), i2, i3);
  }

  public default In2 use3For2 (In1Out1<I3, I2> val) {
    return (i1, i3) -> in(i1, mapper.io(i3), i3);
  }

}
