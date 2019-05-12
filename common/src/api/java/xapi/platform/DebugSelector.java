package xapi.platform;

import xapi.util.X_Runtime;

public class DebugSelector implements PlatformSelector {
  @Override
  public boolean select(Object ... context) {
    return X_Runtime.isDebug();
  }
}