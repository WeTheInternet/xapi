package xapi.ui.api.component;

import xapi.fu.Do;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.itr.EmptyIterator;

/**
 * In order for components to "have children",
 * they must declare insertion points as <slot /> elements.
 *
 * Even if your native elements allow appending children
 * to any element, and it just "won't work right",
 * not all platforms are so forgiving,
 * so we use the abstraction of slot elements
 * (from the web component spec) to declare where children go.
 *
 * The V0 spec used <content select="some.selector" />,
 * which allowed component-driven slotting using a query selector.
 * The V1 spec uses <slot name="myId" />,
 * and then requires the inserted elements to have a slot attribute:
 * <some class="selector" slot="myId" />.
 *
 * In both specs, an unqualified <slot/> / <content /> element
 * would match "anything that isn't otherwise matched".
 *
 * To start with, we will match the V1 spec exactly,
 * but then mix in some querySelector support later,
 * which we will implement with some (generated) manual slotting code.
 *
 * There is one slot controller per element,
 * and it will contain all insertion points,
 * as well as matching a child to the correct parent.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/8/17.
 */
public interface SlotController <ParentEl, ChildEl> {

    SlotController NO_SLOTS = new NoSlotController();

    @SuppressWarnings("unchecked")
    static <ParentEl, ChildEl> SlotController<ParentEl, ChildEl> noSlots() {
        return NO_SLOTS;
    }

    MappedIterable<Slot<ParentEl, ChildEl>> getSlots();

    Maybe<Slot<ParentEl, ChildEl>> findSlot(ChildEl child);

    boolean isPaused();

    void setPaused(boolean paused);

    void whenUnpaused(Do task);

    default void insertChild(ChildEl child) {
        final Do task = ()->{

            final Slot<ParentEl, ChildEl> slot = findSlot(child)
                .getOrThrow(() -> new IllegalArgumentException("No slot found for " + child + " in " + this));
            slot.append(child);

        };
        if (isPaused()) {
            // queue up the insertion for later.
            whenUnpaused(task);
        } else {
            task.done();
        }
    }

    default void setSlotName(ChildEl child) {
        final Do task = ()->{

            final Slot<ParentEl, ChildEl> slot = findSlot(child)
                .getOrThrow(() -> new IllegalArgumentException("No slot found for " + child + " in " + this));
            // When using native shadow dom support with selectors, we do not want to handle any element attachment;
            // instead, we want to simply apply the correct slot="name" attribute to allow native slotting to occur.
            slot.applyName(child);

        };
        if (isPaused()) {
            // queue up the insertion for later.
            whenUnpaused(task);
        } else {
            task.done();
        }
    }

    boolean hasDefaultSlot();

    Slot<ParentEl, ChildEl> getDefaultSlot();

    void setDefaultSlot(Slot<ParentEl, ChildEl> webSlot);

    void addSlot(ParentEl e, String querySelector, boolean nameMode);

    void deslot(ChildEl element);
}
final class NoSlotController<ParentEl, ChildEl> implements SlotController<ParentEl, ChildEl> {

    private boolean paused;
    private Do unpause = Do.NOTHING;

    @Override
    public final MappedIterable<Slot<ParentEl, ChildEl>> getSlots() {
        return EmptyIterator.none();
    }

    @Override
    public void insertChild(ChildEl child) {
        throw new UnsupportedOperationException("Children not allowed");
    }

    @Override
    public boolean hasDefaultSlot() {
        return false;
    }

    @Override
    public Slot<ParentEl, ChildEl> getDefaultSlot() {
        throw new IllegalStateException("No default slot available");
    }

    @Override
    public void setDefaultSlot(Slot<ParentEl, ChildEl> webSlot) {
        throw new UnsupportedOperationException("Cannot add slots to slotless container");
    }

    @Override
    public void addSlot(ParentEl e, String querySelector, boolean nameMode) {
        throw new UnsupportedOperationException("Cannot add slots to slotless container");
    }

    @Override
    public void deslot(ChildEl element) {
        // Not going to throw, just going to ignore, since this could happen if someone hooks up the NoSlot controller
    }

    @Override
    public final Maybe<Slot<ParentEl, ChildEl>> findSlot(ChildEl child) {
        return Maybe.not();
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
}
