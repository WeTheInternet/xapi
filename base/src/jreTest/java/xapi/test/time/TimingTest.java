package xapi.test.time;

import org.junit.Test;

import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.time.api.Moment;

public class TimingTest {

  @Test public void testPrecisionTimer() {
    Moment start = X_Time.now();
    X_Time.trySleep(50, 50);
    Moment end = X_Time.now();
    double diff = end.millis() - start.millis();
    String delta = X_Time.difference(start);
    X_Log.info("Expected sleep time, < 51 millis; actual: "+delta);
    if (diff < 50) {
      X_Log.warn("Time service does not implement sleep");
    } else {
      X_Log.debug("Time service implements sleep correctly.");
    }
    if ((diff - 50) < 0.0001) {
      X_Log.warn("Time service is not high precision");
    } else {
      X_Log.debug("Time service is high precision.");
    }
  }

}
