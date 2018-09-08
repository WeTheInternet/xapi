package xapi.ui.api.component;

import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.api.ElementBuilder;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
public abstract class AbstractComponent <
    El,
    Api extends IsComponent<El>
>
implements IsComponent<El> {

    private final Lazy<El> element;
    private final Lazy<ElementBuilder<El>> self;
    private ComponentOptions<El, Api> opts;
    private String refName;

    public AbstractComponent(El el) {
        this.element = Lazy.withSpy(immutable1(el), this::elementResolved, true);
        initialize(this.element);
        self = Lazy.deferred1(()->newBuilder(this.element));
    }

    public AbstractComponent(Out1<El> element) {
        this.element = Lazy.withSpy(element, this::elementResolved, true);
        initialize(this.element);
        self = Lazy.deferred1(()->newBuilder(this.element));
    }

    @SuppressWarnings("unchecked") //
    public AbstractComponent(ComponentOptions<El, Api> opts, ComponentConstructor<El, Api> constructor) {
        if (opts.needsComponent()) {
            opts.withComponent((Api)this);
        }
        element = Lazy.withSpy(
            Out1.out1Deferred(opts::create, constructor),
            this::elementResolved, true);


        this.opts = opts;
        initialize(element);
        self = Lazy.deferred1(()-> opts.hasBuilder()
            ? opts.getBuilder() : newBuilder(element)
        );
    }


    /**
     * Subtypes that want to access a component as a builder
     * will need to override this method.
     *
     * We give you a Lazy by default, so you can check if it's an instanceof Lazy,
     * if you can be more efficient when it's resolved.
     *
     * @param element The Out1 instance for our element, will be a Lazy, but we'll make it Out1, so you can supply anything
     * @return a new ElementBuilder which understands the platform-specific El type.
     */
    protected ElementBuilder<El> newBuilder(Out1<El> element) {
        throw new UnsupportedOperationException(getClass() + " must implement newBuilder(Out1<El> element)");
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
    protected final void elementResolved(El el) {
        beforeResolved(el);
        onElementResolved(el);
        afterResolved(el);
    }

    protected void beforeResolved(El el) {
    }
    protected void afterResolved(El el) {
    }
    protected void onElementResolved(El el) {
    }

    @Override
    public El getElement() {
        return element.out1();
    }

    @Override
    public boolean isResolving(ElementBuilder<El> builder) {
        if (self.isResolved() && self.out1() == builder) {
            return builder.isResolving();
        }
        return false;
    }

    public boolean isElementResolved() {
        return element.isResolved();
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

    @Override
    public <N extends ElementBuilder> N intoBuilder(IsComponent<?> logicalParent, ComponentOptions opts, N into) {
        final boolean addChild = logicalParent instanceof HasChildren;
        final boolean addParent = this instanceof HasParent;
        if (addChild || addParent) {
            into.onCreated(el->{
                if (addChild) {
                    ((HasChildren) logicalParent).addChildComponent(this);
                }
                if (addParent) {
                    ((HasParent) this).setParent(logicalParent);
                }
            });
        }
        return into;
    }

    @Override
    public ElementBuilder<El> asBuilder() {
        return self.out1();
    }
}
