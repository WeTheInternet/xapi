package xapi.util.api;

/**
 * A simple interface for objects that destroy other objects.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface Destroyer {

  void destroyObject(Object toDestroy);
  
  final Destroyer NO_OP = new NoOp();
  
}

class NoOp implements Destroyer{
  @Override
  public void destroyObject(Object toDestroy) {
  }
}