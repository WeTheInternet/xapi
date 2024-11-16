package xapi.ui.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/23/18.
 */
public interface ElementInjector<Node> {

    void appendChild(Node newChild);

    void insertBefore(Node newChild, Node refChild);

    void insertAtBegin(Node newChild);

    void insertAfter(Node newChild);

    void insertAtEnd(Node newChild);

    void removeChild(Node child);

    void replaceChild(Node newChild, Node refChild);

    Node getParent(Node child);

}
