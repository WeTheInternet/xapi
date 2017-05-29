package xapi.ui.api.component;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.iterate.EmptyIterator;
import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
public abstract class AbstractComponent <
    Node,
    El extends Node,
    Api extends IsComponent<Node, El>
>
implements IsComponent<Node, El> {

    private final Lazy<El> element;
    private ComponentOptions<Node, El, Api> opts;
    private String refName;
    private IsComponent<Node, ? extends Node> parent;
    private IntTo<IsComponent<Node, ? extends Node>> children;

    public AbstractComponent(El element) {
        this(Immutable.immutable1(element));
    }

    @SuppressWarnings("unchecked") //
    public AbstractComponent(ComponentOptions<Node, El, Api> opts, ComponentConstructor<Node, El, Api> constructor) {
        if (opts.needsComponent()) {
            opts.withComponent((Api)this);
        }
        element = Lazy.deferred1(constructor::constructElement, opts);
        this.opts = opts;
        initialize(element);
    }

    public AbstractComponent(Out1<El> element) {
        this.element = Lazy.deferred1(element);
        initialize(this.element);
    }

    protected void initialize(Lazy<El> element) {
    }

    @Override
    public El getElement() {
        return element.out1();
    }

    @Override
    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public ComponentOptions<Node, El, Api> getOpts() {
        return opts;
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
