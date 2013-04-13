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
import xapi.util.api.ConvertsValue;
import xapi.util.api.SuccessHandler;

public class BasicIOTest {

  protected static TestServer server;
  private static final ConvertsValue<String,String> pass_thru = new ConvertsValue<String,String>() {
    @Override
    public String convert(String from) {
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
    IOService service = X_IO.getIOService();
    service.registerParser(String.class, pass_thru, pass_thru);
    IORequestBuilder<String> req = service.request(String.class,
      "http://localhost:" +	TestServer.TEST_PORT+
      "/xapi/debug/");
    IORequest<String> state = req.send(new SuccessHandler<String>() {
      @Override
      public void onSuccess(String t) {
        System.out.println(t);
      }
    });
    X_Log.info(state.isPending());
    while (state.isPending()) {
      X_Time.trySleep(10, 0);
    }

  }


}
