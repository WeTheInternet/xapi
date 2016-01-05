package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public interface TestBufferType <
    Parent extends TestBufferType<? extends GenBuffer, Parent>,
    Self extends TestBufferType<Parent, Self>
    >  extends GenBuffer<Parent, Self> {

  TestBufferAncestor ancestor();


}
