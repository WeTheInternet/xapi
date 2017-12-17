package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/16/17.
 */
public interface HasParent <Node> {

    IsComponent<? extends Node> getParentComponent();

}
