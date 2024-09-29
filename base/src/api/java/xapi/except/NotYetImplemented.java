package xapi.except;

public class NotYetImplemented extends Error{

  private static final long serialVersionUID = -7012257336137767335L;
  @SuppressWarnings("unused") // here for debugging, in case it's an ugly proxy / lambda type
  private final Class<?> source;

  public NotYetImplemented(String message) {
    super(message);
    source = NotYetImplemented.class;
  }
  public NotYetImplemented(Class<?> source, String message) {
    super(source.getName() + " failed to properly implement an api:\n" + message);
    this.source = source;
  }
}
