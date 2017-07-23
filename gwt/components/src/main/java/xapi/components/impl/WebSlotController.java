package xapi.components.impl;

import elemental.dom.Element;
import xapi.components.api.ComponentNamespace;
import xapi.fu.Do;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.SingletonIterator;
import xapi.ui.api.component.Slot;
import xapi.ui.api.component.SlotController;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/8/17.
 */
public class WebSlotController implements SlotController<Element, Element> {

    private final ChainBuilder<Slot<Element, Element>> slots = Chain.startChain();
    private final Element root;
    private Slot<Element, Element> defaultSlot;
    private boolean paused;
    private Do unpause = Do.NOTHING;

    public WebSlotController(Element root) {
        this.root = root;
        JsSupport.flushElementTasks(root);
    }

    public void addSlot(Element parent, String selector, boolean nameMode) {
        if (X_String.isEmpty(selector)) {
            defaultSlot = new WebSlot(parent, "*");
        } else {
            slots.add(new WebSlot(parent, selector, nameMode));
        }
    }

    @Override
    public void deslot(Element element) {
        element.removeAttribute(ComponentNamespace.ATTR_IS_SLOTTED);
    }

    @Override
    public MappedIterable<Slot<Element, Element>> getSlots() {
        if (defaultSlot == null) {
            return slots.caching();
        }
        final MappedIterable<Slot<Element, Element>> start = SingletonIterator.singleItem(defaultSlot);
        return start.append(slots);
    }

    @SuppressWarnings({"unchecked", "RedundantTypeArguments"})
    @Override
    public Maybe<Slot<Element, Element>> findSlot(Element child) {
        Slot[] winner = {defaultSlot};
        final Maybe<String> match = slots
            .spy(s->winner[0] = s)
            .map(Slot::getSelector)
            .firstMatch(JsSupport::matchesSelector, child);
        if (match.isPresent()) {
            return Maybe.<Slot<Element, Element>>immutable(winner[0]);
        }
        return Maybe.nullable(defaultSlot);
    }


    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            unpause.done();
            unpause = Do.NOTHING;
        }
    }

    @Override
    public void whenUnpaused(Do task) {
        if (paused) {
            unpause = unpause.doAfter(task);
        } else {
            task.done();
        }
    }

    @Override
    public Slot<Element, Element> getDefaultSlot() {
        return defaultSlot;
    }

    @Override
    public boolean hasDefaultSlot() {
        return defaultSlot != null;
    }

    public void setDefaultSlot(Slot<Element, Element> defaultSlot) {
        this.defaultSlot = defaultSlot;
    }
}
