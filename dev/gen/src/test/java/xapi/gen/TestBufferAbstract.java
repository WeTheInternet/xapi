package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferAbstract
    < // yes, we try to make these obnoxious generics readable.
        Parent extends TestBufferType<?, Parent>,
        Self extends TestBufferType<Parent, Self>
    >
  extends GenBufferAbstract<Parent, Self>
  implements TestBufferType<Parent, Self>
{

  private final TestBufferAncestor ancestor;

  public TestBufferAbstract(TestBufferAncestor ancestor, Parent parent) {
    super(parent);
    this.ancestor = ancestor;
  }

  @Override
  public TestBufferAncestor ancestor() {
    return ancestor;
  }

}
