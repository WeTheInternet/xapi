package xapi.ui.api.component;

import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public interface IsGraphComponent
    <
        Node,
        El extends Node
    > extends IsComponent<Node, El>
{

    IsComponent<Node, ? extends Node> getParentComponent();

    SizedIterable<IsComponent<Node, ? extends Node>> getChildComponents();

    void setParentComponent(IsComponent<Node, ? extends Node> parent);

    void addChildComponent(IsComponent<Node, ? extends Node> child);

}
