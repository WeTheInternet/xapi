package xapi.process;

import static xapi.process.X_Process.flush;
import static xapi.process.X_Process.kill;
import static xapi.process.X_Process.newThread;
import static xapi.process.X_Process.now;
import static xapi.process.X_Process.runFinally;
import static xapi.process.X_Process.trySleep;

import org.junit.Test;

import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.test.Benchmark;
import xapi.test.Benchmark.Report;
import xapi.test.Benchmark.Timing;
import xapi.util.X_Namespace;
import xapi.util.api.Pointer;
import xapi.util.api.ReceivesValue;


public class ConcurrencyBenchmark {


    static void runBenchmark(final Runnable job, final int iterations, final int maxTime,final Runnable onDone) {
      final double start = now();
      ReceivesValue<Timing> timer = new ReceivesValue<Timing>() {
        @Override
        public void set(final Timing value) {
          //start our threads all at once.  We want to overload the system.
          runFinally(new Runnable() {
            Thread t = newThread(new Runnable() {
              @Override
              public void run() {
                job.run();
                kill(Thread.currentThread(), maxTime);
                value.finish();
              }
            });
            @Override
            public void run() {
              t.start();
            }
          });
        }
      };
      final Pointer<Boolean> block = new Pointer<Boolean>(true);
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
          System.out.println("Started " + iterations+ " threads in "+(now()-start)+" millis");
          System.out.println(Thread.activeCount()+" threads active.");
        }
      };
      int delay = 1;
      System.out.println("Benchmark launched in "+(int)((now()-start)/1000)
        +" seconds.  Active Threads: "+Thread.activeCount());
      while(block.get()) {
        trySleep(delay);
        if (delay < 512) {
          delay <<= 1;
        }else if (delay++%10==0){
          System.out.println("Benchmark has ran for "+(int)((now()-start)/1000)
            +"seconds.  Active Threads: "+Thread.activeCount());
        }
      }
    }


  @Test
  public void benchmarkSynchronizedUnbounded() {
    final double start = now();
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, Integer.toString(Integer.MAX_VALUE));
    final Pointer<Integer> maxThreads = new Pointer<Integer>(0);
    final Pointer<Integer> threadsRan = new Pointer<Integer>(0);
    final int limit = 24000;
    X_Log.logLevel(LogLevel.WARN);
    runBenchmark(new Runnable() {

      @Override
      public void run() {
        int active = Thread.activeCount();
        if (active > maxThreads.get());
          maxThreads.set(active);
        synchronized (threadsRan) {
          threadsRan.set(threadsRan.get()+1);
        }
//        for (int i = 10000000;i-->0;);
      }
    }, limit, 1000, new Runnable() {
      @Override
      public void run() {
        System.out.print("Ran " +threadsRan.get()+" threads in "+(now()-start)+" millis; max threads: "+maxThreads.get());
      }
    });

  }



}
