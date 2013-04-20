package xapi.dev.model;


public class ModelGeneratorContext {

  private boolean isServer;//we do allow runtimes that are both server and client
  private boolean isClient;

  public boolean isClient() {
    return isClient;
  }
  public boolean isServer() {
    return isServer;
  }

  public void setServer(boolean isServer) {
    this.isServer = isServer;
  }

}
