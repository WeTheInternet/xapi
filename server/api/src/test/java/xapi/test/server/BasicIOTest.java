package xapi.test.server;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.io.X_IO;
import xapi.io.api.IORequest;
import xapi.io.api.IORequestBuilder;
import xapi.io.service.IOService;
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ErrorHandler;
import xapi.util.api.Pointer;
import xapi.util.api.SuccessHandler;

import static org.junit.Assert.assertTrue;

public class BasicIOTest {

  private static abstract class Handler implements SuccessHandler<String>, ErrorHandler<Throwable> {
    @Override
    public void onError(final Throwable e) {
      X_Log.error("IO error encountered", e);
      X_Debug.rethrow(e);
    }
  }

  protected static TestServer server;
  private static final ConvertsValue<String,String> pass_thru = new ConvertsValue<String,String>() {
    @Override
    public String convert(final String from) {
      return from;
    }
  };

  @BeforeClass
  public static void setupServer() {
    server = new TestServer();
    server.start();
  }
  @AfterClass
  public static void teardownServer() {
    server.finish();
  }

  @Test
  public void testRequests() {
    final IOService service = X_IO.getIOService();
    service.registerParser(String.class, pass_thru, pass_thru);
    final IORequestBuilder<String> req = service.request(String.class,
      "http://127.0.0.1:" +	TestServer.TEST_PORT+
      "/xapi/debug");
    final Pointer<Boolean> success = new Pointer<Boolean>(false);
    final IORequest<String> state = req.send(new Handler() {
      @Override
      public void onSuccess(final String t) {
        success.set("GET".equals(t));
      }
    });
    while (state.isPending()) {
      X_Time.trySleep(10, 0);
    }
    assertTrue(success.get());

  }


}
