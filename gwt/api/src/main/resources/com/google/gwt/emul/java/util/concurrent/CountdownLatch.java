package java.util.concurrent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CountdownLatch {

  private long count;
  private Logger gwtLogger = null;

  public CountdownLatch(int count) {
    this.count = count;
    gwtLogger = Logger.getLogger("xapi");
  }
  
  public void await(){
    gwtLogger.log(Level.WARNING, "Do not call CountdownLatch.await() in gwt; instead use X_Concurrent.await(latch, callback);");
  }
  public void await(double time, TimeUnit unit){
    gwtLogger.log(Level.WARNING, "Do not call CountdownLatch.await(time, unit) in gwt; instead use X_Concurrent.await(latch, time, unit, callback);");
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
