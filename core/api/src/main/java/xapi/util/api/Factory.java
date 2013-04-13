package xapi.util.api;

public interface Factory <T> {

  MatchesValue<Object> getMatcher();
  ConvertsValue<Object, T> getBuilder();
  
}
