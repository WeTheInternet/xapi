package xapi.components.impl;

import net.wti.lang.parser.ast.expr.UiContainerExpr;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.HTMLCollection;
import elemental2.core.Function;
import elemental2.core.ObjectPropertyDescriptor;
import elemental2.core.Reflect;
import jsinterop.base.Js;
import xapi.collect.fifo.SimpleFifo;
import xapi.components.api.ComponentNamespace;
import xapi.components.api.JsObject;
import xapi.gwt.api.JsLazyExpando;
import xapi.gwt.api.JsObjectDescriptor;
import xapi.components.api.ShadowDomPlugin;
import xapi.fu.In1Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.CachingIterator.ReplayableIterable;
import xapi.gwt.x.X_Gwt;
import xapi.ui.api.component.SlotController;

import static jsinterop.base.Js.cast;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/8/17.
 */
public class ShadowDomControllerBuilder {

    private static final ObjectPropertyDescriptor REAL_INNER_HTML = Reflect.getOwnPropertyDescriptor(
        cast(JsSupport.elementClass().getPrototype())
        , "innerHTML");
    public static final String DISABLE_SHADOW_SPLICING = "shadowLocked";

    private static JsLazyExpando<Element, SlotController<Element, Element>> expando = new JsLazyExpando<>(
        ComponentNamespace.SLOTTER_KEY
    );

    public static final ShadowDomControllerBuilder NO_SLOT_BUILDER = new ShadowDomControllerBuilder(
        e->SlotController.NO_SLOTS,
        WebComponentSupport.hasNativeSupport(),
        false
    );

    private final boolean hasSelector;

    private In1Out1<Element, SlotController<Element, Element>> slotFactory;
    private boolean nativeSupport;
    private ReplayableIterable<Element> shadowRoots;

    public ShadowDomControllerBuilder(WebSlotVisitor visitor) {
        this.slotFactory = visitor.getFactory();
        this.nativeSupport = visitor.isNativeSupport();
        this.hasSelector = visitor.isSawSelector();
    }

    public ShadowDomControllerBuilder(
        In1Out1<Element, SlotController<Element, Element>> factory,
        boolean nativeSupport,
        boolean selector
    ) {
        this.slotFactory = factory;
        this.nativeSupport = nativeSupport;
        this.hasSelector = selector;
    }

    public static ShadowDomControllerBuilder forSource(final UiContainerExpr source, boolean nativeMode) {

        final WebSlotVisitor visitor = new WebSlotVisitor(source)
            .setNativeSupport(nativeMode);

        source.accept(visitor, null);

        if (visitor.getFactory() == visitor.getOriginalFactory()) {
            // no slots in component
            return NO_SLOT_BUILDER;
        }
        if (visitor.isSawDefault() && !visitor.isSawDynamic()) {
            // if there is only one slot, and it is the default,
            // then consider a more optimized factory...
        }

        final ShadowDomControllerBuilder builder = new ShadowDomControllerBuilder(visitor)
            .setNativeSupport(nativeMode);

        if (visitor.isSawSelector() && nativeMode) {
            // when a selector is used, native shadow dom users will still need a mutation observer,
            // and it will choose to set slot attributes, rather than stitch together DOM
        }

        return builder;
    }

    public SlotController<Element, Element> forElement(Element e) {
        return expando.io(e, slotFactory);
    }

    public ShadowDomControllerBuilder setNativeSupport(boolean nativeSupport) {
        this.nativeSupport = nativeSupport;
        return this;
    }

    public boolean isNativeSupport() {
        return nativeSupport;
    }

    public JsLazyExpando<Element, SlotController<Element, Element>> getSlotBinder() {
        return expando;
    }

    public void bindToComponent(
        WebComponentBuilder b,
        ShadowDomControllerBuilder controller,
        In1Out1<Element, Element> initializer,
        ShadowDomPlugin ... plugins
    ) {
        JsLazyExpando<Element, Element> shadyBinder = new JsLazyExpando<>(ComponentNamespace.SHADY_KEY);


        b.afterCreatedCallback(e->{
            // Mark this element as having shadow dom, so you can do *:not([host]) to filter them from query selectors
            e.setAttribute("host", "true");
            // If the element was created with inner text to act as source code,
            // we'll want to pull that out and save it, so later code can see it,
            // without relying on the DOM structure we are about to heavily manipulate
            final HTMLCollection children = e.getChildren();
            if (children.getLength() == 1 && children.item(0).getNodeType() == Node.TEXT_NODE) {
                ComponentNamespace.setSource(e, children.item(0).getTextContent());
            }
        });

        shadyBinder.addToPrototype(b.getPrototype(), element-> {

            // first step, clear any pending tasks this custom element might have,
            // like the afterCreated callback we registered above
            JsSupport.flushElementTasks(element);

            // This will be called once whenever anyone tries to access the shadow DOM.
            // This is where we will handle ripping out existing children, wiring up shadow DOM,
            // then slotting the children we removed.

            // whenever an element is first created, we'll want to grab the existing children and clear them.
            MappedIterable<Node> children = NodeIterable
                .forEach(Js.<elemental2.dom.Element>uncheckedCast(element).childNodes).cached()
                .map(Js::<Node>uncheckedCast);
            if (!children.isEmpty()) {
                element.setAttribute(DISABLE_SHADOW_SPLICING, "true");
                // We don't want this innerHTML write to do the shadow jiggling, as we are handling that here.
                element.setInnerHTML("");
                element.removeAttribute(DISABLE_SHADOW_SPLICING);
            }

            // Then, let our initializer have a peek, and fill out shadow dom element
            Element root = initializer.io(element);
            for (int i = 0; i < element.getChildren().getLength(); i ++) {
                final Node e = element.getChildren().item(i);
                markShadowRoot((Element)e);
            }

            // let the shadow dom plugins look at, but not touch the element (only legal for native shadow dom, and questionable even then)
            assert element == root : "VirtualDom may not return a different element than you were sent.";
            for (ShadowDomPlugin plugin : plugins) {
                final Element newEl = plugin.transform(root, root);
                assert newEl == root : "VirtualDom may not return a different element than you were sent.";
            }
            for (ShadowDomPlugin plugin : b.plugins.forEach()) {
                final Element newEl = plugin.transform(root, root);
                assert newEl == root : "VirtualDom may not return a different element than you were sent.";
            }

            if (controller != ShadowDomControllerBuilder.NO_SLOT_BUILDER) {
                // iff we have a slot controller that actually does something, wire it up!
                b.attachSlotController(controller, element);
            }

            // Now, put back all child elements we removed, so they can be slotted appropriately.
            children.forAll(element::appendChild);
            // TODO any children who were not slotted should be logged somewhere

            return element;
        });

        // virtual shadow root must be resolved by the time we are attached.
        // this ensures that, even if nobody else initialized our shadyRoot,
        // it will be done by the time the element is attached to the page.
        b.attachedCallback(X_Gwt::getShadowRoot);

        // innerHTML is special; we need to get creative for this to work.
        // To get sane semantics, we want to remove all the shadow roots
        b.configureProperty("innerHTML")
            .mutate(existing-> {
                    final JsObjectDescriptor derived = Js.uncheckedCast(JavaScriptObject.createObject());
                    derived.setEnumerable(true);
                    derived.setConfigurable(false);
                    derived.get(REAL_INNER_HTML.getGet());
                    derived.set(JsFunctionSupport.<Element, String>curryThisIn1((e, v)-> {
                            // Before we set the innerHTML, we want to pull out all our shadow DOM elements,
                            // and disable the slotter, so we can put back the shadow DOM, then reslot our elements
                            if ("true".equals(e.getAttribute(DISABLE_SHADOW_SPLICING))) {
                                Js.<Function>cast(REAL_INNER_HTML.getSet())
                                    .call(e, v);
                                return;
                            }
                            X_Gwt.getShadowRoot(e);
                            JsSupport.flushElementTasks(e);
                            MappedIterable<Element> shadowRoots = getShadowRoots(e);
                            final SlotController<Element, Element> slotter;
                            if (controller == NO_SLOT_BUILDER) {
                                slotter = null;
                            } else {
                                final JsLazyExpando<Element, SlotController<Element, Element>> slotBinder = controller.getSlotBinder();
                                slotter = slotBinder.getValue(e);
                                slotter.setPaused(true);
                                slotter.getSlots()
                                    .forAll(s ->
                                        // We have to manually clear each slot, as all non-shadow DOM elements must be purged,
                                        // and we are detaching the slots (and their parents) before setting innerHTML
                                        s.getElement().setInnerHTML("")
                                    );
                            }

                            // actually sets .innerHTML
                            Js.<Function>cast(REAL_INNER_HTML.getSet())
                                .call(e, v);

                            // put back the shadow roots
                            shadowRoots.forAll(e::appendChild);

                            // turn the slotter back on and flush its queue
                            if (slotter != null) {
                                // causes all the queued up re-slots of innerHTML children to trigger, causing them to be slotted
                                slotter.setPaused(false);
                            }
                    }));
                    return derived;

                });


        if (controller != NO_SLOT_BUILDER) {
            controller.getSlotBinder().addToPrototype(b.getPrototype(), e->{
                // Ensure our shadow roots are initialized...
                X_Gwt.getShadowRoot(e);
                // finish any queued tasks
                JsSupport.flushElementTasks(e);
                // grab our slotter from our controller (now that we have initialized)
                final SlotController<Element, Element> slotter = controller.forElement(e);

                return slotter;
            });
        }


    }

    private MappedIterable<Element> getShadowRoots(Element e) {
        SimpleFifo<Element> fifo = new SimpleFifo<>();
        for (int i = 0; i < e.getChildren().length(); i++) {
            final Node child = e.getChildren().item(i);
            if (isShadowRoot(child)) {
                fifo.give((Element)child);
            }
        }
        return fifo;
    }

    private void markShadowRoot(Element element) {
        final JsObjectDescriptor descriptor = JsObjectDescriptor.createUnconfigurable();
        descriptor.get(()->true);
        JsSupport.object().defineProperty(element, ComponentNamespace.SHADOW_ROOT_KEY, descriptor);
    }

    public static boolean isShadowRoot(Node element) {
        return element.getNodeType() == Node.ELEMENT_NODE &&
            Js.<JsObject>uncheckedCast(element).hasProperty(ComponentNamespace.SHADOW_ROOT_KEY);
    }

}
