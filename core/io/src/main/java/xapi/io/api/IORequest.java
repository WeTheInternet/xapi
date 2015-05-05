package xapi.io.api;

import xapi.collect.api.StringTo.Many;

public interface IORequest <V> {

  int STATUS_NOT_HTTP = -4;
  int STATUS_CANCELLED = -3;
  int STATUS_INCOMPLETE = -2;
  int STATUS_UNKNOWN = -1;

  boolean isPending();

  boolean isSuccess();

  void cancel();

  V response();

  Many<String> headers();

  int getStatusCode();

  String getStatusText();

}
