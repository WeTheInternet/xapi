package xapi.process;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.util.X_Namespace;
import xapi.util.api.Pointer;

import static xapi.log.X_Log.*;
import static xapi.process.X_Process.*;
import static xapi.util.X_Properties.*;

public class ConcurrencyTest {

  @BeforeClass public static void prepare() {
    System.setProperty(X_Namespace.PROPERTY_RUNTIME_META, "target/test-classes");
    System.setProperty(X_Namespace.PROPERTY_DEBUG, "true");
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "5");
  }

  @Test
  public void testSimpleDeferment() throws Exception{
    final Pointer<Boolean> success = new Pointer<Boolean>();
    runDeferred(new Runnable() {
      @Override
      public void run() {
        success.set(true);
      }
    });
    success.set(false);
    flush(1000);
    Assert.assertTrue(success.get());
  }
  @Test
  public void testMultiDeferment() throws Exception{
    final Pointer<Boolean> success = new Pointer<Boolean>();
    runFinally(new Runnable() {
      @Override
      public void run() {
        success.set(false);
      }
    });
    runDeferred(new Runnable() {
      @Override
      public void run() {
        success.set(true);
      }
    });
    success.set(false);
    flush(1000);
    Assert.assertTrue(success.get());
  }
  @Test
  public void testComplexDeferment() throws Exception{
    //When launching multiple finalies and defers,
    //we expect all finalies to run between all deferred commands.
    //We will use a closure object to make sure execution is happening in the expected order.
    final Pointer<Long> stage = new Pointer<Long>(0L);
    runFinally(new Runnable() {
      @Override
      public void run() {
        //runs first
        Assert.assertEquals("1st defer ran before 1st finally",stage.get().longValue(), 0);
        stage.set(1L);
        runDeferred(new Runnable() {
          @Override
          public void run() {
            //runs last
            Assert.assertEquals("2nd defer ran before 2nd finally",stage.get().longValue(), 3);
            stage.set(4L);
          }
        });
      }
    });
    runDeferred(new Runnable() {
      @Override
      public void run() {
          //runs second
          Assert.assertEquals("1st defer ran before 1st finally",stage.get().longValue(), 1);
          stage.set(2L);
          runFinally(new Runnable() {
            @Override
            public void run() {
              //runs third, as a finally inside a defer should.
              Assert.assertEquals("2nd finally did not run after 1st defer",stage.get().longValue(), 2);
              stage.set(3L);
            }
        });
      }
    });
    flush(1000);
    Assert.assertTrue(stage.get() == 4);
  }

  @Test
  public void testSimpleThread() {
    final Pointer<Boolean> success = new Pointer<Boolean>();
    Thread t = newThread(new Runnable() {
      @Override
      public void run() {
        info("Thread start time: "+(now()-threadStartTime()));
        //make sure our thread flushed when it's done!
        runFinally(new Runnable() {
          @Override
          public void run() {
            info("To True: "+(now()-threadStartTime()));
            success.set(true);
          }
        });
        info("To False: "+(now()-threadStartTime()));
        success.set(false);
      }
    });
    success.set(false);
    runFinally(new Runnable() {
      @Override
      public void run() {
        info("To False: "+(now()-threadStartTime()));
        success.set(false);
      }
    });
    t.start();
    trySleep(500);
    flush(t, 250);
    Assert.assertTrue(success.get());
  }

  @Test
  public void testThreadTimeout() {
    final Pointer<Boolean> timeout = new Pointer<Boolean>(false);
    final Pointer<Boolean> ran = new Pointer<Boolean>(false);
    Thread first = newThread(new Runnable() {
      @Override
      public void run() {
        ran.set(true);
        runFinally(new Runnable() {
          @Override
          public void run() {
            timeout.set(true);
          }
        });
        trySleep(100);
        timeout.set(false);
      }
    });
    first.start();
    trySleep(50);
    kill(first, 200);
    Assert.assertTrue("Did not run",ran.get());
    Assert.assertTrue("Timing failed",timeout.get());
  }

}
