package xapi.gwt.util;

import xapi.util.service.StringService;

public class StringServiceGwt implements StringService{

  @Override
  public native boolean notNullOrEmpty(String str)
  /*-{
    return str?true:false;
  }-*/;

  @Override
  public native String notNullOrEmpty(String str, String dflt)
  /*-{
    return str || dflt;
  }-*/;

  @Override
  public byte[] toBytes(String str) {
    return str.getBytes();//no charset object; always UTF8
  }

}
