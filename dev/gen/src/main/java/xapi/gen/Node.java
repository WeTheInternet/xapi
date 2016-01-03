package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/2/16.
 */
public class Node <Self extends Node> {

  @SuppressWarnings("unchecked")
  public Self self() {
    return (Self)this;
  }
}
