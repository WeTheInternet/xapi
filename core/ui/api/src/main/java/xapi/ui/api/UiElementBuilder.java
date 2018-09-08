package xapi.ui.api;

import java.util.function.BiFunction;

/**
 * This is a (currently) non-functional builder for UiElement.
 *
 * UiElement was meant to be a universal abstraction for all element types,
 * but it got very burdensome w/ excessive type parameter annoyances,
 * so it's currently back-burnered.  When work on it picks back up,
 * we'll make this builder actually work, to be able to interact with
 * generated IsComponent (where we just work directly on raw element types instead).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/5/18 @ 5:38 AM.
 */
public class UiElementBuilder <U extends UiElement> extends ElementBuilder<U> {

    public UiElementBuilder() {
    }

    public UiElementBuilder(boolean searchableChildren) {
        super(searchableChildren);
    }

    public UiElementBuilder(String tagName) {
        super(tagName);
    }

    public UiElementBuilder(String tagName, boolean searchableChildren) {
        super(tagName, searchableChildren);
    }

    public UiElementBuilder(U element) {
        super(element);
    }

    @Override
    protected StyleApplier createStyleApplier() {
        throw unsupported();
    }

    private RuntimeException unsupported() {
        return new UnsupportedOperationException("UiElementBuilder not implemented yet");
    }

    @Override
    public ElementBuilder<U> createNode(String tagName) {
        throw unsupported();
    }

    @Override
    protected void clearChildren(U element) {
        throw unsupported();
    }

    @Override
    protected U create(CharSequence node) {
        throw unsupported();
    }

    @Override
    protected NodeBuilder<U> wrapChars(CharSequence body) {
        throw unsupported();
    }

    @Override
    protected BiFunction<String, Boolean, NodeBuilder<U>> getCreator() {
        throw unsupported();
    }

    @Override
    public void append(Widget<U> child) {
        throw unsupported();
    }
}
