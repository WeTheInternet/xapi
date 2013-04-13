package xapi.process.impl;



public class IOProcess <T> extends AbstractProcess<T>{

  public static interface Input<T> {
    T input();
  }
  public static interface Output<T> {
    boolean output(T object);
  }
  private Output<T> out;
  public IOProcess(Output<T> output) {
    this.out = output;
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
  public boolean process(float milliTimeLimit) throws Exception {
    return false;
  }




}
