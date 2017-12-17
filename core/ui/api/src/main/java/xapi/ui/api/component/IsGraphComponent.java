package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public interface IsGraphComponent
    <
        Node,
        El extends Node
    > extends IsComponent<El>, HasParent<Node>, HasChildren<Node>
{
//    Inherited
//    IsComponent<? extends Node> getParentComponent();

//    Inherited
//    SizedIterable<IsComponent<? extends Node>> getChildComponents();

    void setParentComponent(IsComponent<? extends Node> parent);

    void addChildComponent(IsComponent<? extends Node> child);

}
