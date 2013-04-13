package xapi.annotation.model;

public enum SerializationStrategy {
  ProtoStream,
  Rpc {
    @Override
    boolean isPatchable() {
      return false;
    }
  },
  ToString {
    @Override
    boolean isPatchable() {
      return false;
    }
  },
  Custom;

  boolean isPatchable() {
    return true;
  }
}
