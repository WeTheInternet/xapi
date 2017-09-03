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

}
