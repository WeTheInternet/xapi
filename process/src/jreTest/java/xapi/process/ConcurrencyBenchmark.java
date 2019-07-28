package xapi.process;

import org.junit.AfterClass;
import org.junit.Test;
import xapi.fu.Mutable;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.test.Benchmark;
import xapi.test.Benchmark.Report;
import xapi.test.Benchmark.Timing;
import xapi.util.X_Namespace;
import xapi.util.api.ReceivesValue;

import static xapi.process.X_Process.*;


public class ConcurrencyBenchmark {

  private static final String was;
  static {
    was = System.getProperty(X_Namespace.PROPERTY_MULTITHREADED);
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, Integer.toString(Integer.MAX_VALUE));
  }

  @AfterClass
  public static void afterClass() {
    if (was == null) {
      System.clearProperty(X_Namespace.PROPERTY_MULTITHREADED);
    } else {
      System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, was);
    }
  }
static int total;
    static void runBenchmark(final Runnable job, final int iterations, final int maxTime,final Runnable onDone) {
      final Double start = now();
      ReceivesValue<Timing> timer = value -> {
        //start our threads all at once.  We want to overload the system.
        Thread t = newThread(() -> {
            job.run();
            kill(Thread.currentThread(), maxTime);
            value.finish();
            synchronized (job) {
              total++;
            }
        });
        runFinally(t::start);
      };
      final Mutable<Boolean> block = new Mutable<>(true);
      new Benchmark(iterations, timer, new Report(System.out)) {
        @Override
        protected void onComplete() {
          super.onComplete();
          onDone.run();
          block.set(false);
        }
        @Override
        protected void onStarted() {
          flush(100000);
          System.out.println(Thread.activeCount()+" threads active.");
        }
      };
      int delay = 1;
      System.out.println("Benchmark launched in "+(int)((now()-start)/1000)
        +" seconds.  Active Threads: "+Thread.activeCount());
      while(block.out1()) {
        trySleep(delay);
        if (delay < 512) {
          delay <<= 1;
        }else if (delay++%10==0){
          System.out.println("Benchmark has run for "+(int)((now()-start)/1000)
            +" seconds.  Active Threads: "+Thread.activeCount());
        }
      }
    }


  @Test(timeout = 60_000)
  public void benchmarkSynchronizedUnbounded() {
    final double start = now();
    final Mutable<Integer> maxThreads = new Mutable<>(0);
    final Mutable<Integer> threadsRan = new Mutable<>(0);
    final int limit = 24000;
    X_Log.logLevel(LogLevel.WARN);
    runBenchmark(() -> {
      int active = Thread.activeCount();
      if (active > maxThreads.out1()) {
        maxThreads.set(active);
      }
        threadsRan.process(i->i+1);
//        for (int i = 10000000;i-->0;);
    }, limit, 1000,
        () -> System.out.print("Ran " +threadsRan.out1()+" threads in "+(now()-start)+" millis; max threads: "+maxThreads.out1())
    );

  }



}
