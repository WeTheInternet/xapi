package xapi.io.api;

import xapi.collect.api.Dictionary;

public interface IORequest <V> {

  boolean isPending();

  boolean isSuccess();

  void cancel();

  V response();

  Dictionary<String,String> headers();

}
