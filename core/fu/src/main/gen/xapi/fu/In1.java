package xapi.fu;

import xapi.annotation.compile.Generated;

@Generated(date="2016-10-12T02:23.193-08:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/In.xapi", "c4ix36x1vxtxkhxt"})
public interface In1 <I1> extends HasInput, Lambda, Rethrowable{

  public abstract void in1 (I1 in1) ;

  public default In1<To> map1 (In1Out1<To, I1> mapper) {
    return (i1) -> in(mapper.io(i1));
  }

  public default Do provide1 (I1 val) {
    return () -> in(val);
  }

  public default In0<I$v> provide1Deferred (Out1<I1> val) {
    return () -> in(val.out1());
  }

  public default In0 provide1Immediate (Out1<I1> val) {
    I1 result = val.out1();
    return () -> in(result);
  }

  public default In2<T, T> require1After (In1<T> require) {
    return (t, i1) -> {
        in(, i1);
        require.in(t);
    };
  }

  public default In2<T, T> require1Before (In1<T> require) {
    return (t, i1) -> {
        require.in(t);
        in(, i1);
    };
  }

  public default In1<I1> spy1Before (In1<I1> spy) {
    return (i1) -> {
      spy.in1(i1);
      in(i1);
    };
  }

  public default In1<I1> spy1After (In1<I1> spy) {
    return (i1) -> {
      in(i1);
      spy.in1(i1);
    };
  }

}
