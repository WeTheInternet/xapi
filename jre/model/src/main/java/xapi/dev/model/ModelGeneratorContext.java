package xapi.dev.model;

import xapi.annotation.inject.PlatformType;

public class ModelGeneratorContext {

  private boolean isServer;//we do allow runtimes that are both server and client
  private boolean isClient;
  private PlatformType platform;

  public boolean isClient() {
    return isClient;
  }
  public boolean isServer() {
    return isServer;
  }

  public void setServer(boolean isServer) {
    this.isServer = isServer;
  }

  public PlatformType getPlatform() {
    return platform;
  }

  public void setPlatform(PlatformType platform) {
    this.platform = platform;
  }


}
