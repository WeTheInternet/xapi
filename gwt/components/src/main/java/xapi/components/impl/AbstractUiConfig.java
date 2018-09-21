package xapi.components.impl;

import elemental.dom.Element;
import elemental.html.StyleElement;
import elemental2.core.JsSet;
import elemental2.dom.DomGlobal;
import jsinterop.base.Js;
import xapi.components.api.UiConfig;
import xapi.elemental.X_Elemental;
import xapi.elemental.api.ElementalService;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.ui.api.StyleAssembler;
import xapi.ui.html.api.GwtStyles;
import xapi.util.api.Destroyable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public abstract class AbstractUiConfig <R extends GwtStyles>
    implements UiConfig<
    Element,
    StyleElement,
    R,
    ElementalService> {

    protected final R styleBundle;
    private final ChainBuilder<In1<Element>> globalCreateListeners;
    private ChainBuilder<StyleAssembler<Element, StyleElement, R, ElementalService>> styles = Chain.startChain();
    private Lazy<MappedIterable<In1Out1<Element, StyleElement>>> factories;
    protected final ElementalService service;
    protected final String namespace;
    private final Out1<StyleAssembler<Element, StyleElement, R, ElementalService>> defaultAssembler;

    public AbstractUiConfig() {
        this(X_Elemental.getElementalService(), GWT.create(GwtStyles.class));
    }

    public AbstractUiConfig forComponent(WebComponentBuilder b) {
        final AbstractUiConfig<R> config = duplicate();
        if (!b.getExtras().has(namespace)) {
            b.getExtras().put(namespace, true);
            b.createdCallback(e->
                config.globalCreateListeners.forAll(In1::in, e)
            );
        }
        config.factories = factories;
        config.globalCreateListeners.addAll(globalCreateListeners);
        return config;
    }

    protected AbstractUiConfig(AbstractUiConfig<R> from) {
        this.styleBundle = from.styleBundle;
        this.service = from.service;
        this.namespace = from.namespace;
        this.defaultAssembler = from.defaultAssembler;
        this.factories = resetFactory();
        globalCreateListeners = initListeners();
    }
    @SuppressWarnings("unchecked")
    public AbstractUiConfig(ElementalService service, R res) {
        this.styleBundle = res;
        this.service = service;
        this.factories = resetFactory();
        globalCreateListeners = initListeners();
        //noinspection NonJREEmulationClassesInClientCode
        namespace = namespacePrefix() + System.identityHashCode(this);
        final Out1<StyleElement> css = Lazy.deferred1(()->service.registerStyle(
            resourceType(),
            getCss(res).getText(),
            cssType()
        ).out1());
        JsSupport.doc().getBody().appendChild(css.out1());
        final StyleAssembler<Element, StyleElement, R, ElementalService> assembler = (styles) ->
            // consider removing the source style element to clone from the dom
            i -> (StyleElement) css.out1().cloneNode(true);
        final Out1<StyleAssembler<Element, StyleElement, R, ElementalService>> wholeStylesheet =
            ()->assembler;
        defaultAssembler = wholeStylesheet;
    }

    protected String namespacePrefix() {
        return "xapi-";
    }

    protected abstract CssResource getCss(R res);

    protected abstract Class<? extends CssResource> cssType();

    protected abstract Class<? extends R> resourceType();

    protected abstract AbstractUiConfig<R> duplicate();


    private ChainBuilder<In1<Element>> initListeners() {
        // TODO: don't use JsSet!  ...move that to xapi-gwt-collect
        JsSet<String> seen = new JsSet<>();
        return Chain.toChain(e->
            factories.out1().forEach(
                factory->
                    deferExecution(()->{
                        final Element into;
                        if ("true".equals(e.getAttribute("host"))) {
                            // when the element is a host, it means we want to insert our stylesheet into the body,
                            // and we only want to insert it once per element type.
                            if (seen.has(e.getTagName())) {
                                return;
                            }
                            seen.add(e.getTagName());
                            into = Js.uncheckedCast(DomGlobal.document.body);
                        } else {
                            if (X_Elemental.hasShadowRoot(e)) {
                                into = X_Elemental.getShadowRoot(e);
                            } else {
                                into = e;
                            }
                        }
                        final StyleElement styleElement = factory.io(e);
                        if (into.getFirstElementChild() == null) {
                            into.appendChild(styleElement);
                        } else {
                            // insert after any <style /> elements,
                            // but before any other elements.
                            Element target = into.getFirstElementChild();
                            while (
                                "style".equals(target.getTagName().toLowerCase())
                                    && target.getNextElementSibling() != null
                                ) {
                                target = target.getNextElementSibling();
                            }
                            into.insertBefore(styleElement, target);
                        }
                    })
            ));
    }

    protected void deferExecution(Do task) {
        RunSoon.schedule(task);
    }

    protected Lazy<MappedIterable<In1Out1<Element, StyleElement>>> resetFactory() {
        if (factories != null) {
            factories.ifFull(items->items.forEach(factory->{
                if (factory instanceof Destroyable) {
                    // release anything which holds onto resources
                    // i.e., big gnarly dom elements or old compilation models
                    ((Destroyable)factory).destroy();
                }
            }));
        }
        return Lazy.deferred1(()->
            styles
                .map(a->a.styleInjector(service))
                // uses a copy of the mapped iterable,
                // so we only make one injector per assembler.
                // note that assemblers will be recreated if new ones are added...
                .copy()
        );
    }

    @Override
    public void addStyleAssembler(StyleAssembler<Element, StyleElement, R, ElementalService> assembler) {
        styles.add(assembler);
        factories = resetFactory();
    }

    public R getResources() {
        return styleBundle;
    }

    @Override
    public StyleAssembler<Element, StyleElement, R, ElementalService> getDefaultAssembler() {
        return defaultAssembler.out1();
    }
}
