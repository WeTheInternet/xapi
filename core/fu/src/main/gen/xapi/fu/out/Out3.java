package xapi.fu.out;

import xapi.annotation.compile.Generated;
import xapi.fu.HasOutput;
import xapi.fu.Lambda;
import xapi.fu.Rethrowable;

@Generated(date="2017-05-24T02:45.133-07:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/Out.xapi", "a3uszsw84euvxpzr"})
public interface Out3 <O1, O2, O3> extends HasOutput, Lambda, Rethrowable{


    Immutable3<Out1<O1>, Out1<O2>, Out1<O3>> outAll();


    default O1 out1() {
      return out1Provider().out1();
    }


    default O2 out2() {
      return out2Provider().out1();
    }


    default O3 out3() {
      return out3Provider().out1();
    }


    default Out1<O1> out1Provider() {
      return outAll().out1();
    }


    default Out1<O2> out2Provider() {
      return outAll().out2();
    }


    default Out1<O3> out3Provider() {
      return outAll().out3();
    }


    default Out3<O1, O2, O3> read1(In1<O1> callback) {
      callback.in(out1());
      return this;
    }


    default Out3<O1, O2, O3> read2(In1<O2> callback) {
      callback.in(out2());
      return this;
    }


    default Out3<O1, O2, O3> read3(In1<O3> callback) {
      callback.in(out3());
      return this;
    }



    default <To> Out3<To, O2, O3> mapped1(In1Out1<O1, To> mapper) {
      return Out.out3(mapper.supplyDeferred(out1Provider()), out2Provider(), out3Provider());
    }



    default <To> Out3<O1, To, O3> mapped2(In1Out1<O2, To> mapper) {
      return Out.out3(out1Provider(), mapper.supplyDeferred(out2Provider()), out3Provider());
    }



    default <To> Out3<O1, O2, To> mapped3(In1Out1<O3, To> mapper) {
      return Out.out3(out1Provider(), out2Provider(), mapper.supplyDeferred(out3Provider()));
    }


    default <To> Out3<O1, O2, O3> spy1(In1<O1> callback) {
      return Out.out3(out1Provider().spy1(callback), out2Provider(), out3Provider());
    }


    default <To> Out3<O1, O2, O3> spy2(In1<O2> callback) {
      return Out.out3(out1Provider(), out2Provider().spy1(callback), out3Provider());
    }


    default <To> Out3<O1, O2, O3> spy3(In1<O3> callback) {
      return Out.out3(out1Provider(), out2Provider(), out3Provider().spy1(callback));
    }
  }
