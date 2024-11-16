package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public interface IsGraphComponent
    <
        Node,
        El extends Node
    > extends IsComponent<El>, HasParent<IsComponent<? extends Node>>, HasChildren<IsComponent<? extends Node>>
{
//    Inherited from HasParent
//    IsComponent<? extends Node> getParent();
//    void setParent(IsComponent<? extends Node> parent);

//    Inherited from HasChildren
//    SizedIterable<IsComponent<? extends Node>> getChildComponents();
//    void addChildComponent(IsComponent<? extends Node> child);

}
