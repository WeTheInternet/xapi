package xapi.ui.api.component;

import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/16/17.
 */
public interface HasChildren <Node> {

    SizedIterable<Node> getChildComponents();

    void addChildComponent(Node child);

    void removeChild(Node me);

}
