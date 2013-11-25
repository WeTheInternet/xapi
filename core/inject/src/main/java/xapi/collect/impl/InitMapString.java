package xapi.collect.impl;

import xapi.util.api.ConvertsValue;

public class InitMapString <Value> extends InitMapDefault<String, Value> {

  public InitMapString(ConvertsValue<String, Value> initializer) {
    super( PASS_THRU, initializer);
  }
}
