package xapi.source.write;

/**
 * A simple interface for converting objects into strings.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ToStringer {

  final ToStringer DEFAULT_TO_STRINGER = new DefaultToStringer();
  
  String toString(Object o);
  
}
final class DefaultToStringer implements ToStringer {
  @Override
  public String toString(Object o) {
    return String.valueOf(o);
  }
}