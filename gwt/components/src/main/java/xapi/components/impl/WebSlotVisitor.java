package xapi.components.impl;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import elemental.dom.Element;
import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.ui.api.component.SlotController;

import static xapi.string.X_String.dequote;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/15/17.
 */
class WebSlotVisitor extends VoidVisitorAdapter<Object> {
    private final In1Out1<Element, SlotController<Element, Element>> originalFactory;
    private final UiContainerExpr source;
    private In1Out1<Element, SlotController<Element, Element>> factory;
    private boolean sawDefault;
    private boolean sawDynamic;
    private boolean sawSelector;
    private boolean nativeSupport;

    public WebSlotVisitor(UiContainerExpr source) {
        this.source = source;
        factory = originalFactory = getDefaultFactory();
    }

    protected In1Out1<Element, SlotController<Element, Element>> getDefaultFactory() {
        return WebSlotController::new;
    }

    @Override
    public void visit(UiContainerExpr n, Object arg) {
        if (n.getName().equalsIgnoreCase("slot")) {
            final In1Out1<Element, SlotController<Element, Element>> prevFactory = factory;
            // found a slot.  Does it have a selector / name?
            final Maybe<UiAttrExpr> name = n.getAttribute("name");
            if (name.isAbsent()) {
                final Maybe<UiAttrExpr> selector = n.getAttribute("selector");
                if (selector.isAbsent()) {
                    // found the default slot.
                    if (sawDefault) {
                        throw new IllegalStateException("Cannot have more than one default <slot /> in " + source.toSource());
                    }
                    sawDefault = true;
                    factory = e -> {
                        final SlotController<Element, Element> me = prevFactory.io(e);
                        final Element defaultSlot = e.querySelector("slot:not([name]):not([selector])");
                        assert defaultSlot != null : "Could not find default slot";
                        me.setDefaultSlot(new WebSlot(defaultSlot));
                        return me;
                    };
                } else {
                    sawSelector = sawDynamic = true;
                    // a slot with a selector.
                    String querySelector = dequote(selector.get().getExpression().toSource());
                    factory = e -> {
                        final SlotController<Element, Element> me = prevFactory.io(e);
                        final Element parent = e.querySelector("slot[selector=\"" + querySelector + "\"]");
                        assert parent != null : "Could not find parent slot with selector=" + querySelector;
                        me.addSlot(parent, querySelector, false);
                        return me;
                    };
                }
            } else {
                sawDynamic = true;
                // a slot with a name
                String slotName = dequote(name.get().getExpression().toSource());
                factory = e -> {
                    final SlotController<Element, Element> me = prevFactory.io(e);
                    final Element parent = e.querySelector("slot[name=" + slotName + "]");
                    assert parent != null : "Could not find parent slot with name=" + slotName;
                    me.addSlot(parent, slotName, true);
                    return me;
                };
            }
        }
        super.visit(n, arg);
    }

    public In1Out1<Element, SlotController<Element, Element>> getOriginalFactory() {
        return originalFactory;
    }

    public UiContainerExpr getSource() {
        return source;
    }

    public In1Out1<Element, SlotController<Element, Element>> getFactory() {
        return factory;
    }

    public boolean isSawDefault() {
        return sawDefault;
    }

    public boolean isSawDynamic() {
        return sawDynamic;
    }

    public boolean isSawSelector() {
        return sawSelector;
    }

    public boolean isNativeSupport() {
        return nativeSupport;
    }

    public WebSlotVisitor setNativeSupport(boolean nativeSupport) {
        this.nativeSupport = nativeSupport;
        return this;
    }
}
