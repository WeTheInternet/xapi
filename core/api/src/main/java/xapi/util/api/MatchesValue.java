package xapi.util.api;

public interface MatchesValue <T> {

  @SuppressWarnings("rawtypes")
  final MatchesValue ANY = new MatchesValue(){
    public boolean matches(Object value) {
      return true;
    }
  };

  @SuppressWarnings("rawtypes")
  final MatchesValue NONE = new MatchesValue(){
    public boolean matches(Object value) {
      return false;
    }
  };

  boolean matches(T value);

}
