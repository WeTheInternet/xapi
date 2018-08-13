package xapi.process.impl;

import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.process.X_Process;
import xapi.process.api.ProcessCursor;
import xapi.process.api.RescheduleException;
import xapi.util.X_Util;

import java.util.concurrent.TimeUnit;

public class IOProcess <T> extends AbstractProcess<T>{

  In1<T> in;
  Out1<T> out;
  public IOProcess(In1<T> input, Out1<T> output) {
    this.out = output;
    this.in = input;
  }

//  public boolean process(Input<T> provider) throws Exception {
//    onStart();
//    try {
//      T value, previous = null;
//      boolean success = true;
//      while ((value = provider.input())!=null) {
//        if (value == previous)
//          break;
//        success &= out.output(value);
//        previous = value;
//      }
//      return success;
//    }finally {
//      onStop();
//    }
//  }

//  public boolean process(T object) throws Exception {
//    onStart();
//    try {
//      return out.output(object);
//    }finally {
//      onStop();
//    }
//  }

  @Override
  public boolean process(float milliTimeLimit) {
    X_Process.scheduleInterruption((long)(1_000_000. * milliTimeLimit), TimeUnit.NANOSECONDS);
    try {
      final T value = out.out1();
      in.in(value);
      return true;
    } catch (Throwable t) {
      final Throwable unwrapped = X_Util.unwrap(t);
      if (unwrapped instanceof InterruptedException) {
        return false;
      }
      if (unwrapped instanceof RescheduleException) {
        // client wants to reschedule the process
        RescheduleException r = (RescheduleException) unwrapped;
        // TODO have a reschedule mode which may include "allow retry immediately"
        final ProcessCursor cursor = r.getCursor();
        return false;
      }
      // Any other exception is getting thrown.
      throw t;
    }
  }




}
