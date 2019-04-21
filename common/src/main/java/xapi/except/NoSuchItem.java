package xapi.except;

/**
 * A fatal error that happens when an invalid key is used for some operation.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class NoSuchItem extends RuntimeException implements IsFatal{

  private final Object key;

  protected NoSuchItem() {//for serialization only
    key = null;
  }

  public NoSuchItem(Object key) {
    super("No entity exists with key " + key);
    this.key = key;
  }

  public NoSuchItem(Object key, Throwable cause) {
    super("No entity exists with key " + key, cause);
    this.key = key;
  }

  public Object getKey() {
    return key;
  }
}
