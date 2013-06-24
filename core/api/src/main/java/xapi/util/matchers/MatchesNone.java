package xapi.util.matchers;

import xapi.util.api.MatchesValue;

public class MatchesNone <T> implements MatchesValue <T>{

  @SuppressWarnings("rawtypes")
  private static final MatchesAll INST = new MatchesAll();
  
  @SuppressWarnings("unchecked") // Always safe, we never touch value
  public static <T> MatchesAll<T> getInstance() {
    return INST;
  }
  
  @Override
  public boolean matches(T value) {
    return false;
  }

  
}
