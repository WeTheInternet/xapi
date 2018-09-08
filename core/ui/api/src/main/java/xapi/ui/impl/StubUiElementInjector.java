package xapi.ui.impl;

import xapi.ui.api.UiElement;
import xapi.ui.api.UiInjector;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/5/18 @ 5:50 AM.
 */
public class StubUiElementInjector implements UiInjector {

    private final UiElement el;

    public StubUiElementInjector(UiElement el) {
        this.el = el;
    }

    @Override
    public void appendChild(Object newChild) {
        throw unsupported();
    }

    @Override
    public void insertBefore(Object newChild, Object refChild) {
        throw unsupported();
    }

    @Override
    public void insertAtBegin(Object newChild) {
        throw unsupported();
    }

    @Override
    public void insertAfter(Object newChild) {
        throw unsupported();
    }

    @Override
    public void insertAtEnd(Object newChild) {
        throw unsupported();
    }

    @Override
    public void removeChild(Object child) {
        throw unsupported();
    }

    private RuntimeException unsupported() {
        return new UnsupportedOperationException("stub");
    }
}
