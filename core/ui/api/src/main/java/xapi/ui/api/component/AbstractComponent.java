package xapi.ui.api.component;

import xapi.fu.*;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
public abstract class AbstractComponent <
    El,
    Api extends IsComponent<El>
>
implements IsComponent<El> {

    private final Lazy<El> element;
    private ComponentOptions<El, Api> opts;
    private String refName;

    public AbstractComponent(El element) {
        this(Immutable.immutable1(element));
    }

    @SuppressWarnings("unchecked") //
    public AbstractComponent(ComponentOptions<El, Api> opts, ComponentConstructor<El, Api> constructor) {
        if (opts.needsComponent()) {
            opts.withComponent((Api)this);
        }
        element = Lazy.deferred1(In1Out1.of(constructor::constructElement).supply(opts)
            .spy1(this::elementResolved));
        this.opts = opts;
        initialize(element);
    }


    public AbstractComponent(Out1<El> element) {
        this.element = Lazy.deferred1(element.spy1(this::elementResolved));
        initialize(this.element);
    }

    protected void initialize(Lazy<El> element) {
    }

    /**
     * Called when our Lazy element provider is resolved.
     *
     * (when an element is passed to our constructor,
     * the element could already be created for a long time;
     * when the component is creating its own element,
     * this will be some of the first code to execute on said element).
     *
     * If you intend to override this, ensure you call .getElement() to trigger Lazy resolution!
     * (in many cases, getElement() will be called for you by other infrastructure,
     * so do be careful in assuming that it will be called in _all_ cases).
     *
     * @param el - The element this component manipulates.
     */
    protected void elementResolved(El el) {
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

    public ComponentOptions<El, Api> getOpts() {
        return opts;
    }

}
