package xapi.ui.api.component;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Out1;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;
import xapi.model.api.Model;

/**
 * A branching component (panel with parent / child) and a model.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 12/16/17.
 */
public abstract class AbstractGraphModelComponent <Node, El extends Node, Mod extends Model,
    Api extends IsGraphComponent<Node, El> & IsModelComponent<El, Mod>>
    extends AbstractModelComponent<El, Mod, Api>
    implements IsGraphComponent<Node, El> {

    private IsComponent<? extends Node> parent;
    private IntTo<IsComponent<? extends Node>> children;

    public AbstractGraphModelComponent(El element) {
        super(element);
    }

    public AbstractGraphModelComponent(
        ModelComponentOptions<El, Mod, Api> opts,
        ComponentConstructor<El, Api> constructor
    ) {
        super(opts, constructor);
    }

    public AbstractGraphModelComponent(Out1<El> element) {
        super(element);
    }

    public void setParent(IsComponent<? extends Node> parent) {
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addChildComponent(IsComponent<? extends Node> child) {
        if (this.children == null) {
            this.children = X_Collect.newList(Class.class.cast(IsComponent.class));
        }
        this.children.add(child);
    }

    @Override
    public IsComponent<? extends Node> getParent() {
        return parent;
    }

    @Override
    public SizedIterable<IsComponent<? extends Node>> getChildComponents() {
        return children == null ? EmptyIterator.none() : children.forEachItem();
    }

}
