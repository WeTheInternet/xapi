package xapi.dev.model;

import xapi.platform.JrePlatform;
import xapi.platform.Platform;

@JrePlatform
public class ModelGeneratorContext {

  private boolean isServer;//we do allow runtimes that are both server and client
  private boolean isClient;
  // stay non-null, thanks
  private Platform platform = initPlatform();

  public boolean isClient() {
    return isClient;
  }

  protected Platform initPlatform() {
    return ModelGeneratorContext.class.getAnnotation(Platform.class);
  }
  public boolean isServer() {
    return isServer;
  }

  public void setServer(boolean isServer) {
    this.isServer = isServer;
  }

  public Platform getPlatform() {
    return platform;
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }


}
