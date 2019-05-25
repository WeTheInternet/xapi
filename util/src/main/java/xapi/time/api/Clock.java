package xapi.time.api;

import xapi.util.X_String;

public class Clock{
  long startMilli;
  long startNano;
  long doneMilli;
  long doneNano;
  public Clock() {
    start();
  }
  public Clock(long startMilli, long startNano, long doneMilli, long doneNano) {
    this.startMilli = startMilli;
    this.startNano = startNano;
    this.doneMilli = doneMilli;
    this.doneNano = doneNano;
  }

  public double getTime() {
    double time = doneMilli - startMilli;
    if (time < 25) {
      //only calculate nanoe precision after 25 millis!
      return ((double)Math.abs(doneNano-startNano))/1000000000.0;
    }
    return time / 1000.0;
  }
  public Clock start() {
    startMilli = System.currentTimeMillis();
    startNano = System.nanoTime();
    return this;
  }
  public Clock stop() {
    doneNano = System.nanoTime();
    doneMilli = System.currentTimeMillis();
    return this;
  }

  @Override
  public String toString() {
    if (doneMilli==0)stop();
    return X_String.toMetricSuffix(getTime())+" seconds";
  }

}