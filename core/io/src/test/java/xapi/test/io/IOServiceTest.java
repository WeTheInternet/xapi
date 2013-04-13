package xapi.test.io;

import org.junit.Test;

import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.impl.IOServiceDefault;
import xapi.io.service.IOService;

public class IOServiceTest {

  protected IOService service() {
    return new IOServiceDefault();
  }

  @Test
  public void testGet() {
    service().get("http://google.com", null, new IOCallback<IOMessage<String>>() {

      @Override
      public void onError(Throwable e) {
        throw new RuntimeException(e);
      }

      @Override
      public void onSuccess(IOMessage<String> t) {
      }

      @Override
      public void onCancel() {
        throw new RuntimeException();
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    });
  }


}
