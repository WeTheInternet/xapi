package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public interface IsGraphComponent
    <
        Node,
        El extends Node,
        Self extends IsGraphComponent<Node, El, Self>
    > extends IsComponent<Node, El>
{

}
