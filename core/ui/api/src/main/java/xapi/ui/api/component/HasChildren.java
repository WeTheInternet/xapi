package xapi.ui.api.component;

import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/16/17.
 */
public interface HasChildren <Node> {

    SizedIterable<IsComponent<? extends Node>> getChildComponents();

    void addChildComponent(IsComponent<? extends Node> child);

    void removeChild(IsComponent<? extends Node> me);

}
