package xapi.components.impl;

import elemental.dom.Element;
import elemental.dom.Node;
import xapi.components.api.ComponentNamespace;
import xapi.dev.debug.NameGen;
import xapi.ui.api.component.Slot;
import xapi.string.X_String;

import com.google.gwt.core.client.GWT;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/8/17.
 */
public class WebSlot implements Slot<Element, Element> {

    private final Element element;
    private final String selector;
    private final boolean named;

    public WebSlot(Element parent) {
        this(parent, "*");
    }
    public WebSlot(Element parent, String selector) {
        this(parent, selector, false);
    }

    public WebSlot(Element parent, String selector, boolean nameMode) {
        this.element = parent;
        this.selector = X_String.isEmpty(selector) ? "*" : selector;
        this.named = nameMode;
    }

    @Override
    public String getSelector() {
        return "*".equals(selector) ? selector :
            isNamed() ? "*[slot=" + selector + "]" : selector;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void append(Element child) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
            if ("true".equals(child.getAttribute(ComponentNamespace.ATTR_IS_SLOTTED))) {
                GWT.debugger();
            } else {
                child.setAttribute(ComponentNamespace.ATTR_IS_SLOTTED, "true");
                element.appendChild(child);
            }
        } else {

            element.appendChild(child);
        }
    }

    @Override
    public void applyName(Element child) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
            child.setAttribute("slot", calculateName());
        }
    }

    private String calculateName() {
        String name = element.getAttribute("name");
        if (name == null) {
            name = element.getId();
            if (name == null) {
                name = NameGen.getGlobal().newName("slot");
            }
            element.setAttribute("name", name);
        }
        return name;
    }

    public boolean isNamed() {
        return named;
    }
}
