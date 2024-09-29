package xapi.util.api;

public interface Destroyable extends AutoCloseable {

  void destroy();

  @Override
  default void close() {
    destroy();
  }
}
