package xapi.util.api;

import xapi.util.matchers.MatchesAll;
import xapi.util.matchers.MatchesNone;

public interface MatchesValue <T> {

  @SuppressWarnings("rawtypes")
  final MatchesValue ANY = new MatchesAll();

  @SuppressWarnings("rawtypes")
  final MatchesValue NONE = new MatchesNone();

  boolean matches(T value);

}
