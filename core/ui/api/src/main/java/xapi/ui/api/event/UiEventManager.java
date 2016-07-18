package xapi.ui.api.event;

import xapi.event.api.EventManager;
import xapi.event.api.HasBeforeAfter;
import xapi.event.api.IsEvent;
import xapi.event.api.IsEventType;
import xapi.event.impl.EventTypes;
import xapi.fu.Filter.Filter2;
import xapi.fu.Out3;
import xapi.model.X_Model;
import xapi.ui.api.UiElement;
import xapi.ui.service.UiService;

import javax.validation.constraints.NotNull;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/17/16.
 */
public class UiEventManager <Node, Ui extends UiElement<Node, ? extends Node, Ui>> extends EventManager {

    public class UiEventChain {
        Ui self;
        UiEventChain next;
        UiEventChain prev;

        public UiEventChain(Ui source) {
            self = source;
        }
    }

    private final UiService<Node, Ui> service;

    public UiEventManager(UiService<Node, Ui> service) {
        this.service = service;
    }

    @Override
    public boolean fireEvent(@NotNull IsEvent<?> event) {
        if (event instanceof UiEvent) {
            // When we are dealing with a UiEvent, we want to run a capture/bubble phase over ancestors.
            return fireUiEvent((UiEvent<?, Node, Ui>)event);
        } else {
            return super.fireEvent(event);
        }
    }

    public <Payload> boolean fireUiEvent(Ui node, IsEventType type, Payload payload) {
        UiEventContext<Payload, Node, Ui> context = newContext(node, type, payload);
        UiEvent<Payload, Node, Ui> event = createEvent(type, context);
        return fireUiEvent(event);
    }

    protected <Payload> UiEventContext<Payload,Node,Ui> newContext(Ui node, IsEventType type, Payload payload) {
        final UiEventContext<Payload, Node, Ui> ctx = X_Model.create(UiEventContext.class);
        ctx.setEventType(type.getEventType());
        ctx.setNativeEventTarget(node.element());
        ctx.setNativeType(type.getEventType());
        ctx.setPayload(payload);
        ctx.setSource(node);
        return ctx;
    }

    public <Payload> boolean fireUiEvent(Node nativeNode, String nativeType, Payload payload) {
        Ui ui = service.findContainer(nativeNode);
        if (ui == null) {
            throw new IllegalStateException("Unable to find registered container for " + nativeNode + " in " + service.debugDump());
        }
        final IsEventType type = service.convertType(nativeType);
        UiEventContext<Payload, Node, Ui> context = newContext(ui, type, payload);
        context.setNativeEventTarget(nativeNode);
        context.setNativeType(nativeType);
        UiEvent<Payload, Node, Ui> event = createEvent(type, context);
        return fireUiEvent(event);
    }

    protected <Payload> UiEvent<Payload,Node,Ui> createEvent(IsEventType type, UiEventContext<Payload, Node, Ui> context) {
        if (type instanceof EventTypes) {
            switch ((EventTypes)type) {
                case Click:
                    return (UiClickEvent<Payload, Node, Ui>)()->context;
                case LongClick:
                    return (UiLongClickEvent<Payload, Node, Ui>)()->context;
                case DoubleClick:
                    return (UiDoubleClickEvent<Payload, Node, Ui>)()->context;
                case Move:
                    return (UiMoveEvent<Payload, Node, Ui>)()->context;
                case Scroll:
                    return (UiScrollEvent<Payload, Node, Ui>)()->context;
                case Hover:
                    return (UiHoverEvent<Payload, Node, Ui>)()->context;
                case Unhover:
                    return (UiUnhoverEvent<Payload, Node, Ui>)()->context;

                case Focus:
                    return (UiFocusEvent<Payload, Node, Ui>)()->context;
                case Blur:
                    return (UiBlurEvent<Payload, Node, Ui>)()->context;

                case DragStart:
                    return (UiDragStartEvent<Payload, Node, Ui>)()->context;
                case DragEnd:
                    return (UiDragEndEvent<Payload, Node, Ui>)()->context;
                case DragMove:
                    return (UiDragMoveEvent<Payload, Node, Ui>)()->context;

                case Resize:
                    return (UiResizeEvent<Payload, Node, Ui>)()->context;
                case Attach:
                    return (UiAttachEvent<Payload, Node, Ui>)()->context;
                case Detach:
                    return (UiDetachEvent<Payload, Node, Ui>)()->context;

                case Select:
                    return (UiSelectEvent<Payload, Node, Ui>)()->context;
                case Unselect:
                    return (UiUnselectEvent<Payload, Node, Ui>)()->context;

                case Change:
                    Payload payload = context.getPayload();
                    assert payload instanceof HasBeforeAfter : "Change events must supply a payload which implements HasBeforeAfter";
                    HasBeforeAfter values = (HasBeforeAfter)payload;
                    return (UiChangeEvent<Payload, Node, Ui, Object>)()-> Out3.out3(context, values::getBefore, values::getAfter);

                case Undo:
                    payload = context.getPayload();
                    assert payload instanceof HasBeforeAfter : "Undo events must supply a payload which implements HasBeforeAfter";
                    values = (HasBeforeAfter)payload;
                    return (UiUndoEvent<Payload, Node, Ui, Object>)()-> Out3.out3(context, values::getBefore, values::getAfter);
            }
        }
        // No pre-canned event type to support... create an anonymous event class
        return new UiEvent<Payload, Node, Ui>() {
            @Override
            public UiEventContext<Payload, Node, Ui> getSource() {
                return context;
            }

            @Override
            public IsEventType getType() {
                return type;
            }
        };
    }

    protected boolean fireUiEvent(UiEvent<?, Node, Ui> event) {
        final UiEventContext<?, Node, Ui> context = event.getSource();
        if (context == null) {
            throw new IllegalStateException("UiEvent context not initialized: " + event);
        }
        Ui source = context.getSourceElement();
        if (source == null) {
            throw new IllegalStateException("UiEvent source not initialized: " + event);
        }

        UiEventChain captureChain = buildUiChain(event, new UiEventChain(source), UiElement::handlesCapture);
        while (captureChain != null) {
            if (!captureChain.self.onEventCapture(event)) {
                return false;
            }
            captureChain = captureChain.next;
        }

        UiEventChain bubbleChain = new UiEventChain(source);
        buildUiChain(event, bubbleChain, UiElement::handlesCapture);
        while (bubbleChain != null) {
            if (!bubbleChain.self.onEventBubble(event)) {
                return false;
            }
            bubbleChain = captureChain.prev;
        }
        
        return true;
    }

    protected UiEventChain buildUiChain(UiEvent<?, Node, Ui> event, UiEventChain node,
                                        Filter2<Object, UiElement<Node, ? extends Node, Ui>, UiEvent<?, Node, Ui>> filter) {
        Ui ui = node.self;
        while (ui.getParent() != null) {
            Ui parent = ui.getParent();
            if (filter.filter2(parent, event)) {
                UiEventChain prev = new UiEventChain(parent);
                node.prev = prev;
                prev.next = node;
                node = prev;
            }
            ui = parent;
        }
        return node;
    }
}
