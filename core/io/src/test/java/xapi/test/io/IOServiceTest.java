package xapi.test.io;

import java.net.UnknownHostException;

import org.junit.Test;

import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.impl.IOServiceDefault;
import xapi.io.service.IOService;
import xapi.util.X_Util;


public class IOServiceTest {

  protected IOService service() {
    return new IOServiceDefault();
  }

  @Test
  public void testGet() {
    try{
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
    } catch (Throwable e){
      if (X_Util.unwrap(e) instanceof UnknownHostException)
        return;
      throw X_Util.rethrow(e);   
    }
  }


}
