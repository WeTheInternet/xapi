package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/16/17.
 */
public interface HasParent <Node> {

    Node getParent();

    default void setParent(Node node) {
        throw new UnsupportedOperationException("Parent immutable");
    }
}
