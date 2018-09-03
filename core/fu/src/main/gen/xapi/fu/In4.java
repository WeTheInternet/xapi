package xapi.fu;

import xapi.annotation.compile.Generated;

@Generated(date="2016-10-12T02:23.189-08:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/In.xapi", "wiaa5n7xhpw2jucy"})
public interface In4 <I1, I2, I3, I4> extends HasInput, Lambda, Rethrowable{

  public abstract void in4 (I1 in1, I2 in2, I3 in3, I4 in4) ;

  public default In4<I4, I4, I4, I4> map1 (In1Out1<To, I1> mapper) {
    return (i1, i2, i3, i4) -> in(mapper.io(i1), i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4, I4> merge1And2 (In1Out1<T, I1> map1, In1Out1<T, I2> map2) {
    return (t, i3, i4) -> in(map1.io(i1), map2.io(i2), i3, i4);
  }

  public default In3<I4, I4, I4, I4, I4> merge1And3 (In1Out1<T, I1> map1, In1Out1<T, I3> map3) {
    return (t, i2, i4) -> in(map1.io(i1), i2, map3.io(i3), i4);
  }

  public default In3 merge1And4 (In1Out1<T, I1> map1, In1Out1<T, I4> map4) {
    return (t, i2, i3) -> in(map1.io(i1), i2, i3, map4.io(i4));
  }

  public default In3<I$v, I$v, I$v, I$v> provide1 (I1 val) {
    return (, i2, i3, i4) -> in(val, i2, i3, i4);
  }

  public default In3<I$v, I$v, I$v, I$v> provide1Deferred (Out1<I1> val) {
    return (, i2, i3, i4) -> in(val.out1(), i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> provide1Immediate (Out1<I1> val) {
    I1 result = val.out1();
    return (, i2, i3, i4) -> in(result, i2, i3, i4);
  }

  public default In5<I3, I3, I3, I3, I3> require1After (In1<T> require) {
    return (t, i1, i2, i3, i4) -> {
        in(, i1, i2, i3, i4);
        require.in(t);
    };
  }

  public default In5<I3, I3, I3, I3, I3> require1Before (In1<T> require) {
    return (t, i1, i2, i3, i4) -> {
        require.in(t);
        in(, i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy1Before (In1<I1> spy) {
    return (i1, i2, i3, i4) -> {
      spy.in1(i1);
      in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy1After (In1<I1> spy) {
    return (i1, i2, i3, i4) -> {
      in(i1, i2, i3, i4);
      spy.in1(i1);
    };
  }

  public default In4<I4, I4, I4, I4> swap1And2 () {
    return (i2, i1, i3, i4) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use1For2 (In1Out1<I1, I2> val) {
    return (i1, i3, i4) -> in(i1, mapper.io(i1), i3, i4);
  }

  public default In4<I4, I4, I4, I4> swap1And3 () {
    return (i3, i2, i1, i4) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use1For3 (In1Out1<I1, I3> val) {
    return (i1, i2, i4) -> in(i1, i2, mapper.io(i1), i4);
  }

  public default In4<I1, I1, I1, I1> swap1And4 () {
    return (i4, i2, i3, i1) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use1For4 (In1Out1<I1, I4> val) {
    return (i1, i2, i3) -> in(i1, i2, i3, mapper.io(i1));
  }

  public default In4<I4, I4, I4, I4> map2 (In1Out1<To, I2> mapper) {
    return (i1, i2, i3, i4) -> in(i1, mapper.io(i2), i3, i4);
  }

  public default In3<I4, I4, I4, I4, I4> merge2And3 (In1Out1<T, I2> map2, In1Out1<T, I3> map3) {
    return (t, i1, i4) -> in(i1, map2.io(i2), map3.io(i3), i4);
  }

  public default In3 merge2And4 (In1Out1<T, I2> map2, In1Out1<T, I4> map4) {
    return (t, i1, i3) -> in(i1, map2.io(i2), i3, map4.io(i4));
  }

  public default In3<I$v, I$v, I$v, I$v> provide2 (I2 val) {
    return (i1, i3, i4) -> in(i1, val, i3, i4);
  }

  public default In3<I$v, I$v, I$v, I$v> provide2Deferred (Out1<I2> val) {
    return (i1, i3, i4) -> in(i1, val.out1(), i3, i4);
  }

  public default In3<I4, I4, I4, I4> provide2Immediate (Out1<I2> val) {
    I2 result = val.out1();
    return (i1, i3, i4) -> in(i1, result, i3, i4);
  }

  public default In5<I3, I3, I3, I3, I3> require2After (In1<T> require) {
    return (i1, t, i2, i3, i4) -> {
        in(i1, i2, i3, i4);
        require.in(t);
    };
  }

  public default In5<I3, I3, I3, I3, I3> require2Before (In1<T> require) {
    return (i1, t, i2, i3, i4) -> {
        require.in(t);
        in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy2Before (In1<I2> spy) {
    return (i1, i2, i3, i4) -> {
      spy.in1(i2);
      in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy2After (In1<I2> spy) {
    return (i1, i2, i3, i4) -> {
      in(i1, i2, i3, i4);
      spy.in1(i2);
    };
  }

  public default In3<I4, I4, I4, I4> use2For1 (In1Out1<I2, I1> val) {
    return (, i2, i3, i4) -> in(mapper.io(i2), i2, i3, i4);
  }

  public default In4<I4, I4, I4, I4> swap2And3 () {
    return (i1, i3, i2, i4) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use2For3 (In1Out1<I2, I3> val) {
    return (i1, i2, i4) -> in(i1, i2, mapper.io(i2), i4);
  }

  public default In4<I2, I2, I2, I2> swap2And4 () {
    return (i1, i4, i3, i2) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use2For4 (In1Out1<I2, I4> val) {
    return (i1, i2, i3) -> in(i1, i2, i3, mapper.io(i2));
  }

  public default In4<I4, I4, I4, I4> map3 (In1Out1<To, I3> mapper) {
    return (i1, i2, i3, i4) -> in(i1, i2, mapper.io(i3), i4);
  }

  public default In3 merge3And4 (In1Out1<T, I3> map3, In1Out1<T, I4> map4) {
    return (t, i1, i2) -> in(i1, i2, map3.io(i3), map4.io(i4));
  }

  public default In3<I$v, I$v, I$v, I$v> provide3 (I3 val) {
    return (i1, i2, i4) -> in(i1, i2, val, i4);
  }

  public default In3<I$v, I$v, I$v, I$v> provide3Deferred (Out1<I3> val) {
    return (i1, i2, i4) -> in(i1, i2, val.out1(), i4);
  }

  public default In3<I4, I4, I4, I4> provide3Immediate (Out1<I3> val) {
    I3 result = val.out1();
    return (i1, i2, i4) -> in(i1, i2, result, i4);
  }

  public default In5<I3, I3, I3, I3, I3> require3After (In1<T> require) {
    return (i1, i2, t, i3, i4) -> {
        in(i1, i2, i3, i4);
        require.in(t);
    };
  }

  public default In5<I3, I3, I3, I3, I3> require3Before (In1<T> require) {
    return (i1, i2, t, i3, i4) -> {
        require.in(t);
        in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy3Before (In1<I3> spy) {
    return (i1, i2, i3, i4) -> {
      spy.in1(i3);
      in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy3After (In1<I3> spy) {
    return (i1, i2, i3, i4) -> {
      in(i1, i2, i3, i4);
      spy.in1(i3);
    };
  }

  public default In3<I4, I4, I4, I4> use3For1 (In1Out1<I3, I1> val) {
    return (, i2, i3, i4) -> in(mapper.io(i3), i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use3For2 (In1Out1<I3, I2> val) {
    return (i1, i3, i4) -> in(i1, mapper.io(i3), i3, i4);
  }

  public default In4<I3, I3, I3, I3> swap3And4 () {
    return (i1, i2, i4, i3) -> in(i1, i2, i3, i4);
  }

  public default In3<I4, I4, I4, I4> use3For4 (In1Out1<I3, I4> val) {
    return (i1, i2, i3) -> in(i1, i2, i3, mapper.io(i3));
  }

  public default In4<To, To, To, To> map4 (In1Out1<To, I4> mapper) {
    return (i1, i2, i3, i4) -> in(i1, i2, i3, mapper.io(i4));
  }

  public default In3<I$v, I$v, I$v, I$v> provide4 (I4 val) {
    return (i1, i2, i3) -> in(i1, i2, i3, val);
  }

  public default In3<I$v, I$v, I$v, I$v> provide4Deferred (Out1<I4> val) {
    return (i1, i2, i3) -> in(i1, i2, i3, val.out1());
  }

  public default In3 provide4Immediate (Out1<I4> val) {
    I4 result = val.out1();
    return (i1, i2, i3) -> in(i1, i2, i3, result);
  }

  public default In5<T, T, T, T, T> require4After (In1<T> require) {
    return (i1, i2, i3, t, i4) -> {
        in(i1, i2, i3, i4);
        require.in(t);
    };
  }

  public default In5<T, T, T, T, T> require4Before (In1<T> require) {
    return (i1, i2, i3, t, i4) -> {
        require.in(t);
        in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy4Before (In1<I4> spy) {
    return (i1, i2, i3, i4) -> {
      spy.in1(i4);
      in(i1, i2, i3, i4);
    };
  }

  public default In4<I4, I4, I4, I4> spy4After (In1<I4> spy) {
    return (i1, i2, i3, i4) -> {
      in(i1, i2, i3, i4);
      spy.in1(i4);
    };
  }

  public default In3 use4For1 (In1Out1<I4, I1> val) {
    return (, i2, i3, i4) -> in(mapper.io(i4), i2, i3, i4);
  }

  public default In3 use4For2 (In1Out1<I4, I2> val) {
    return (i1, i3, i4) -> in(i1, mapper.io(i4), i3, i4);
  }

  public default In3 use4For3 (In1Out1<I4, I3> val) {
    return (i1, i2, i4) -> in(i1, i2, mapper.io(i4), i4);
  }

}
