package java.util.concurrent;

import xapi.log.X_Log;

public class CountdownLatch {

  private long count;

  public CountdownLatch(int count) {
    this.count = count;
  }
  
  public void await(){
    X_Log.warn("Do not call CountdownLatch.await() in gwt; instead use X_Concurrent.await(latch, callback);");
  }
  public void await(double time, TimeUnit unit){
    X_Log.warn("Do not call CountdownLatch.await(time, unit) in gwt; instead use X_Concurrent.await(latch, time, unit, callback);");
  }

  /**
   * @return the count
   */
  public long getCount() {
    return count;
  }
  
  public void countDown(){
    count--;
  }
  
  @Override
  public String toString() {
    return "CountDownLatch("+count+")";
  }
  
}
