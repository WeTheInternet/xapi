package xapi.platform;

public interface PlatformSelector {

  boolean select(Object ... context);

  class AlwaysTrue implements PlatformSelector {
    @Override
    public boolean select(Object ... context) {
      return true;
    }
  }

}
