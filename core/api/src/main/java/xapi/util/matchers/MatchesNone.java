package xapi.util.matchers;

import xapi.util.api.MatchesValue;

public class MatchesNone <T> implements MatchesValue <T>{

  @SuppressWarnings("unchecked") // Always safe, we never touch value
  public static <T> MatchesValue<T> getInstance() {
    return NONE;
  }

  @Override
  public boolean matches(T value) {
    return false;
  }


}
