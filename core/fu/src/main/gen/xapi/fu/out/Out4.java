package xapi.fu.out;

import xapi.annotation.compile.Generated;
import xapi.fu.HasOutput;
import xapi.fu.Lambda;
import xapi.fu.Rethrowable;

@Generated(date="2017-05-24T02:45.146-07:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/Out.xapi", "rxit0c75ycmvum6z"})
public interface Out4 <O1, O2, O3, O4> extends HasOutput, Lambda, Rethrowable{


    Immutable4<Out1<O1>, Out1<O2>, Out1<O3>, Out1<O4>> outAll();


    default O1 out1() {
      return out1Provider().out1();
    }


    default O2 out2() {
      return out2Provider().out1();
    }


    default O3 out3() {
      return out3Provider().out1();
    }


    default O4 out4() {
      return out4Provider().out1();
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


    default Out1<O4> out4Provider() {
      return outAll().out4();
    }


    default Out4<O1, O2, O3, O4> read1(In1<O1> callback) {
      callback.in(out1());
      return this;
    }


    default Out4<O1, O2, O3, O4> read2(In1<O2> callback) {
      callback.in(out2());
      return this;
    }


    default Out4<O1, O2, O3, O4> read3(In1<O3> callback) {
      callback.in(out3());
      return this;
    }


    default Out4<O1, O2, O3, O4> read4(In1<O4> callback) {
      callback.in(out4());
      return this;
    }



    default <To> Out4<To, O2, O3, O4> mapped1(In1Out1<O1, To> mapper) {
      return Out.out4(mapper.supplyDeferred(out1Provider()), out2Provider(), out3Provider(), out4Provider());
    }



    default <To> Out4<O1, To, O3, O4> mapped2(In1Out1<O2, To> mapper) {
      return Out.out4(out1Provider(), mapper.supplyDeferred(out2Provider()), out3Provider(), out4Provider());
    }



    default <To> Out4<O1, O2, To, O4> mapped3(In1Out1<O3, To> mapper) {
      return Out.out4(out1Provider(), out2Provider(), mapper.supplyDeferred(out3Provider()), out4Provider());
    }



    default <To> Out4<O1, O2, O3, To> mapped4(In1Out1<O4, To> mapper) {
      return Out.out4(out1Provider(), out2Provider(), out3Provider(), mapper.supplyDeferred(out4Provider()));
    }


    default <To> Out4<O1, O2, O3, O4> spy1(In1<O1> callback) {
      return Out.out4(out1Provider().spy1(callback), out2Provider(), out3Provider(), out4Provider());
    }


    default <To> Out4<O1, O2, O3, O4> spy2(In1<O2> callback) {
      return Out.out4(out1Provider(), out2Provider().spy1(callback), out3Provider(), out4Provider());
    }


    default <To> Out4<O1, O2, O3, O4> spy3(In1<O3> callback) {
      return Out.out4(out1Provider(), out2Provider(), out3Provider().spy1(callback), out4Provider());
    }


    default <To> Out4<O1, O2, O3, O4> spy4(In1<O4> callback) {
      return Out.out4(out1Provider(), out2Provider(), out3Provider(), out4Provider().spy1(callback));
    }
  }
