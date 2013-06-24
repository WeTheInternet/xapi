package xapi.util.converters;

import xapi.util.api.ConvertsValue;

public class ConvertsStringValue <E> implements ConvertsValue<E, String> {

  @Override
  public String convert(E from) {
    return String.valueOf(from);
  }
}
