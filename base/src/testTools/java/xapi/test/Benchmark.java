package xapi.test;

import xapi.time.api.Clock;
import xapi.util.api.ReceivesValue;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static xapi.string.X_String.toMetricSuffix;

public class Benchmark {

  public static class Report implements ReceivesValue<List<Timing>>{

    private PrintWriter out;

    public Report(OutputStream out) {
      this.out = new PrintWriter(out);
    }

    @Override
    public void set(List<Timing> value) {
      if (value.isEmpty()) {
        out.println("Zero results; zero timing.");
        return;
      }
      double size = value.size();//no integer math please!
      int s = (int) size;
      out.println("Calculating timing for " + s + " results.");
      //calculate max, min, mean and median
      double max = 0, min = Double.MAX_VALUE, avg = 0;
      double first = value.get(0).getTime();
      double last = value.get(s-1).getTime();
      for (Timing timing : value.toArray(new Timing[s])) {
        double time = timing.getTime();
        max = Math.max(max, time);
        min = Math.min(min, time);
        avg += time / size;
      }
      int maxPos=0, minPos=0;
      for (int i = value.size(); i-->0; ) {
        if (value.get(i).getTime() == max) {
          maxPos = i;
          if (minPos > 0)break;
        }
        if (value.get(i).getTime() == min) {
          minPos = i;
          if (maxPos > 0)break;
        }
      }
      out.println("Max: "+toMetricSuffix(max) + " ["+maxPos+"]");
      out.println("Min: "+toMetricSuffix(min) + " [" +minPos+"]");
      out.println("First: "+toMetricSuffix(first));
      out.println("Last: "+toMetricSuffix(last));
      out.println("Mean: "+toMetricSuffix(avg));

      value.sort((o1, o2) -> (int) ((1000000) * (o1.getTime() - o2.getTime())));
      double median = value.get((value.size()/2)).getTime();

      out.println("Median: "+toMetricSuffix(median));
      out.flush();

    }
  }

  public class Timing extends Clock {
    @SuppressWarnings("WeakerAccess")
    public Timing() {
    }
    public void finish() {
      int val;
      synchronized (Benchmark.this) {
        done ++;
        val = done;
      }
      stop();
//      System.out.println("Done "+val+" of "+limit);
      if (val == limit) {
        onComplete.run();
      }
    }
  }
  public final List<Timing> results;
  private final int limit;
  private volatile int done = 0;
  private final Runnable onComplete;

  public Benchmark(int iterations, ReceivesValue<Timing> job, final ReceivesValue<List<Timing>> onDone) {
    results = new ArrayList<>(iterations);
    limit = iterations;
    this.onComplete = () -> {
      onDone.set(results);
      onComplete();
    };
    while (iterations --> 0) {
      Timing t = new Timing();
      results.add(t);
      job.set(t);
    }
    onStarted();
  }


  protected void onStarted() {

  }

  protected void onComplete() {
  }


}
