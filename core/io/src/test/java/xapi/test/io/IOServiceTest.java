package xapi.test.io;

import java.net.UnknownHostException;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.io.api.IORequest;
import xapi.io.service.IOService;
import xapi.jre.io.IOServiceDefault;
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Util;
import xapi.util.api.Pointer;


public class IOServiceTest {

  protected IOService service() {
    return new IOServiceDefault();
  }

  @Test
  public void testGet() {
    final Moment now = X_Time.now();
    final Pointer<Boolean> success = new Pointer<Boolean>(false);
    IORequest<String> response;
    try{
    response = service().get("http://httpbin.org/get", null, t -> {
      Assert.assertNotNull(t.body());
      Assert.assertNotSame(0, t.body().length());
      success.set(true);
    });
    } catch (final Throwable e){
      if (X_Util.unwrap(e) instanceof UnknownHostException) {
        // Computer is offline. Ignore.
        return;
      }
      throw X_Util.rethrow(e);
    }
    response.response();
    Assert.assertTrue(success.get());
    System.out.println("Test took "+X_Time.difference(now));
  }

  @Test
  public void testPost() {
    final Moment now = X_Time.now();
    final Pointer<Boolean> success = new Pointer<Boolean>(false);
    IORequest<String> response;
    try{
      response = service().post("http://httpbin.org/post",
          "test=success",
          null, t -> {
        Assert.assertNotNull(t.body());
        Assert.assertNotSame(0, t.body().length());
        final JSONObject asJson = new JSONObject(t.body());
        success.set(true);
        X_Log.info(t.body());
        Assert.assertEquals("success", asJson.getJSONObject("form").getString("test"));
      });
    } catch (final Throwable e){
      if (X_Util.unwrap(e) instanceof UnknownHostException) {
        // Computer is offline. Ignore.
        return;
      }
      throw X_Util.rethrow(e);
    }
    response.response();
    Assert.assertTrue(success.get());
    System.out.println("Test took "+X_Time.difference(now));
  }

  @Test
  @Ignore("Use a local server instead of flaky jsontest.com")
  public void testHeaders() {
    final Moment now = X_Time.now();
    final Pointer<Boolean> success = new Pointer<Boolean>(false);
    IORequest<String> response;
    try{
      final StringDictionary<String> headers = X_Collect.newDictionary();
      headers.setValue("test", "success");
      response = service().get("http://headers.jsontest.com", headers, t -> {
        Assert.assertNotNull(t.body());
        Assert.assertNotSame(0, t.body().length());
        final JSONObject asJson = new JSONObject(t.body());
        Assert.assertEquals("success", asJson.getString("test"));
        Assert.assertEquals("*", t.headers().get("Access-Control-Allow-Origin").at(0));
        success.set(true);
      });
    } catch (final Throwable e){
      if (X_Util.unwrap(e) instanceof UnknownHostException) {
        // Computer is offline. Ignore.
        return;
      }
      throw X_Util.rethrow(e);
    }
    response.response();
    Assert.assertTrue(success.get());
    System.out.println("Test took "+X_Time.difference(now));
  }


}
