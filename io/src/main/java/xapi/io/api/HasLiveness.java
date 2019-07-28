package xapi.io.api;

/**
 * Simple interfaces for types that need to be polled for liveness.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface HasLiveness {

  boolean isAlive();
  
}
