package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/12/15.
 */
public class NodeWithParent<Parent extends GenBuffer <?, Parent>, Self extends GenBuffer <Parent, Self>> {


  protected NodeWithParent<?, Parent> parent;

  protected Self node;

  protected NodeWithParent() {

  }

  protected NodeWithParent(Self node) {
    this();
    this.node = node;
  }

}
