package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface HasInput {

  int accept(int index, Object ... values);

  default InMany toMany() {
    return InMany.of(this);
  }
}
