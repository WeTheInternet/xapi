package xapi.ui.api.component;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Out1;
import xapi.fu.iterate.EmptyIterator;
import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public class AbstractGraphComponent <
    Node,
    El extends Node,
    Api extends IsComponent<Node, El>
    >
    extends AbstractComponent<Node, El, Api>
    implements IsGraphComponent<Node, El> {

    private IsComponent<Node, ? extends Node> parent;
    private IntTo<IsComponent<Node, ? extends Node>> children;

    public AbstractGraphComponent(El element) {
        super(element);
    }

    public AbstractGraphComponent(
        ComponentOptions<Node, El, Api> opts,
        ComponentConstructor<Node, El, Api> constructor
    ) {
        super(opts, constructor);
    }

    public AbstractGraphComponent(Out1<El> element) {
        super(element);
    }

    @Override
    public void setParentComponent(IsComponent<Node, ? extends Node> parent) {
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addChildComponent(IsComponent<Node, ? extends Node> child) {
        if (this.children == null) {
            this.children = X_Collect.newList(Class.class.cast(IsComponent.class));
        }
        this.children.add(child);
    }

    @Override
    public IsComponent<Node, ? extends Node> getParentComponent() {
        return parent;
    }

    @Override
    public SizedIterable<IsComponent<Node, ? extends Node>> getChildComponents() {
        return children == null ? EmptyIterator.none() : children.forEachItem();
    }

}
