package xapi.fu.out;

import xapi.annotation.compile.Generated;
import xapi.fu.HasOutput;
import xapi.fu.Lambda;
import xapi.fu.Rethrowable;

@Generated(date="2017-05-24T02:45.148-07:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/Out.xapi", "7qhb7yqshcfv96f1"})
public interface Out2 <O1, O2> extends HasOutput, Lambda, Rethrowable{


    Immutable2<Out1<O1>, Out1<O2>> outAll();


    default O1 out1() {
      return out1Provider().out1();
    }


    default O2 out2() {
      return out2Provider().out1();
    }


    default Out1<O1> out1Provider() {
      return outAll().out1();
    }


    default Out1<O2> out2Provider() {
      return outAll().out2();
    }


    default Out2<O1, O2> read1(In1<O1> callback) {
      callback.in(out1());
      return this;
    }


    default Out2<O1, O2> read2(In1<O2> callback) {
      callback.in(out2());
      return this;
    }



    default <To> Out2<To, O2> mapped1(In1Out1<O1, To> mapper) {
      return Out.out2(mapper.supplyDeferred(out1Provider()), out2Provider());
    }



    default <To> Out2<O1, To> mapped2(In1Out1<O2, To> mapper) {
      return Out.out2(out1Provider(), mapper.supplyDeferred(out2Provider()));
    }


    default <To> Out2<O1, O2> spy1(In1<O1> callback) {
      return Out.out2(out1Provider().spy1(callback), out2Provider());
    }


    default <To> Out2<O1, O2> spy2(In1<O2> callback) {
      return Out.out2(out1Provider(), out2Provider().spy1(callback));
    }
  }
