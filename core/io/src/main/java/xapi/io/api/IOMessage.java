package xapi.io.api;

import xapi.collect.api.StringTo.Many;

public interface IOMessage <B> {

  int modifier();

  String url();

  Many<String> headers();

  B body();

  int statusCode();

  String statusMessage();

}
