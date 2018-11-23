package xapi.process;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.fu.Mutable;
import xapi.util.X_Namespace;
import xapi.util.api.Pointer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static xapi.log.X_Log.info;
import static xapi.process.X_Process.*;

public class ConcurrencyTest {

  @BeforeClass public static void prepare() {
    System.setProperty(X_Namespace.PROPERTY_RUNTIME_META, "target/test-classes");
    System.setProperty(X_Namespace.PROPERTY_DEBUG, "true");
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "5");
  }

  @Test(timeout = 20_000)
  public void testSimpleDeferment() throws Exception{
    final Pointer<Boolean> success = new Pointer<Boolean>();
    runDeferred(()->success.set(true));
    success.set(false);
    flush(5000);
    Assert.assertTrue(success.get());
  }
  @Test(timeout = 20_000)
  public void testMultiDeferment() throws Exception{
    final Pointer<Boolean> success = new Pointer<Boolean>();
    runFinally(()->success.set(false));
    runDeferred(()->success.set(true));
    success.set(false);
    flush(5000);
    Assert.assertTrue(success.get());
  }
  @Test(timeout = 35_000)
  public void testComplexDeferment() throws Exception{
    //When launching multiple finalies and defers,
    //we expect all finalies to run between all deferred commands.
    //We will use a closure object to make sure execution is happening in the expected order.
    final Pointer<Long> stage = new Pointer<Long>(0L);
    CountDownLatch latch = new CountDownLatch(2);
    synchronized (stage) {
        Thread.yield();
        // toss in a memory barrier and a yield to encourage the JVM to pause if we are running out of quorum.
        // That is, if our thread is about to pause, we do _not_ want it to pause between our first two schedules.
        // So, we encourage the jvm to stop here.  No big deal when running single threaded,
        // but our build runs in parallel, so native OS is more likely to be busy enough to want to stop our thread.
        runFinally(() -> {
            //runs first
            Assert.assertEquals("1st defer ran before 1st finally",stage.get().longValue(), 0);
            stage.set(1L);
            runDeferred(() -> {
                //runs last
                Assert.assertEquals("2nd defer ran before 2nd finally",stage.get().longValue(), 3);
                stage.set(4L);
                latch.countDown();
            });
        });
        runDeferred(() -> {
              //runs second
              Assert.assertEquals("1st defer ran before 1st finally",stage.get().longValue(), 1);
              stage.set(2L);
              runFinally(() -> {
                  //runs third, as a finally inside a defer should.
                  Assert.assertEquals("2nd finally did not run after 1st defer",stage.get().longValue(), 2);
                  stage.set(3L);
                  latch.countDown();
            });
        });
    }

    do {
        flush(200);
    } while (!latch.await(100, TimeUnit.MILLISECONDS));

    Assert.assertTrue(stage.get() == 4);
  }

  @Test(timeout = 10_000)
  public void testSimpleThread() throws InterruptedException {
    final Mutable<Boolean> success = new Mutable<>();
    Semaphore wait = new Semaphore(1, true);
    wait.acquire();
    Semaphore finish = new Semaphore(1);
    finish.acquire();
    Thread t = newThreadUnsafe(() -> {
        info("Thread start time: "+(now()-threadStartTime()));
        //make sure our thread flushed when it's done!
        wait.acquire();
        runFinallyUnsafe(() -> {
            wait.acquire();
            info("To True: "+(now()-threadStartTime()));
            success.set(true);
            finish.release();
        });
        info("To False 2: "+(now()-threadStartTime()));
        success.set(false);
        wait.release();
    });
    success.set(false);
    runFinally(() -> {
        info("To False 1: "+(now()-threadStartTime()));
        success.set(false);
        wait.release();
    });
    t.start();
    while (success.out1() != Boolean.TRUE) {
        trySleep(100);
        flush(t, 250);
    }

    Assert.assertTrue(success.out1());
  }

  @Test
  public void testThreadTimeout() {
    final Pointer<Boolean> timeout = new Pointer<Boolean>(false);
    final Pointer<Boolean> ran = new Pointer<Boolean>(false);
    Thread first = newThread(() -> {
        ran.set(true);
        runFinally(() ->timeout.set(true));
        timeout.set(false);
    });
    first.start();
    trySleep(150);
    kill(first, 2500);
    Assert.assertTrue("Did not run",ran.get());
    Assert.assertTrue("Timing failed",timeout.get());
  }

}
