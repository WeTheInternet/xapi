package xapi.fu.out;

import xapi.annotation.compile.Generated;
import xapi.fu.HasOutput;
import xapi.fu.Lambda;
import xapi.fu.Rethrowable;

@Generated(date="2017-05-24T02:45.148-07:00",
  value = {"xapi.dev.api.ApiGenerator", "xapi/fu/Out.xapi", "s93qkjihoecucscb"})
public interface Out1 <O1> extends HasOutput, Lambda, Rethrowable{


    Immutable1<Out1<O1>> outAll();


    default O1 out1() {
      return out1Provider().out1();
    }


    default Out1<O1> out1Provider() {
      return outAll().out1();
    }


    default Out1<O1> read1(In1<O1> callback) {
      callback.in(out1());
      return this;
    }



    default <To> Out1<To> mapped1(In1Out1<O1, To> mapper) {
      return Out.out1(mapper.supplyDeferred(out1Provider()));
    }


    default <To> Out1<O1> spy1(In1<O1> callback) {
      return 
  () -> {
    O1 out = out1();
    callback.in(out);
    return out;
  }
;
    }
  }
