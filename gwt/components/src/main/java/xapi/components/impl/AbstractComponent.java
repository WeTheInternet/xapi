package xapi.components.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.iterate.EmptyIterator;
import xapi.fu.iterate.SizedIterable;
import xapi.ui.api.UiNode;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponent;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
public abstract class AbstractComponent <
    Node,
    El extends Node,
    BaseType extends UiNode<Node, ? extends Node, BaseType>,
    Api extends IsComponent<El, Api>
>
implements UiNode<Node, El, BaseType>, IsComponent<El, Api>{

    private final Lazy<El> element;
    private ComponentOptions<El, Api> opts;
    private BaseType parent;
    private IntTo<BaseType> children;
    private String refName;

    public AbstractComponent(El element) {
        this(Immutable.immutable1(element));
    }

    public void setParent(BaseType parent) {
        this.parent = parent;
    }

    public AbstractComponent(ComponentOptions<El, Api> opts, ComponentConstructor<El, Api> constructor) {
        if (opts.needsComponent()) {
            opts.withComponent(getUi());
        }
        element = Lazy.deferred1(constructor::construct, opts);
        this.opts = opts;
        initialize(element);
    }

    public AbstractComponent(Out1<El> element) {
        this.element = Lazy.deferred1(element);
        initialize(element);
    }

    protected void initialize(Out1<El> element) {
    }

    @Override
    public BaseType getParent() {
        return parent;
    }

    @Override
    public El getElement() {
        return element.out1();
    }

    public BaseType getComponent() {
        return (BaseType) this;
    }

    @Override
    public SizedIterable<BaseType> getChildren() {
        return children == null ? EmptyIterator.none() : children
            .forEachItem().counted();
    }

    @Override
    public Api getUi() {
        return (Api) this;
    }

    @Override
    public void setParentComponent(IsComponent<?, ?> parent) {
        this.parent = (BaseType) parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addChildComponent(IsComponent<?, ?> child) {
        if (this.children == null) {
            this.children = X_Collect.newList(Class.class.cast(IsComponent.class));
        }
        this.children.add((BaseType)child);
    }

    @Override
    public IsComponent<?, ?> getParentComponent() {
        // totally not safe... gotta fix dem generics :-/
        return (IsComponent<?, ?>) parent;
    }

    @Override
    public MappedIterable<IsComponent<?, ?>> getChildComponents() {
        return children == null ? EmptyIterator.none() : children.forEachItem()
            .map(i->(IsComponent<?, ?>)i);
    }

    @Override
    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }
}
