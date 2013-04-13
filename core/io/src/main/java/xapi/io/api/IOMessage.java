package xapi.io.api;

import xapi.collect.api.StringDictionary;

public interface IOMessage <B> {

  int modifier();

  String url();

  StringDictionary<String> headers();

  B body();

}
