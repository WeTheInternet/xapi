package xapi.ui.impl;

import xapi.ui.api.ElementBuilder;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.Widget;

import java.util.function.BiFunction;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/10/17.
 */
public class StringElementBuilder extends ElementBuilder<String> {

    public StringElementBuilder(CharSequence body) {
        el = body.toString();
    }

    public StringElementBuilder(CharSequence body, boolean searchableChildren) {
        super(searchableChildren);
        el = body.toString();
    }

    @Override
    public void append(Widget<String> child) {
        if (child.getElement() != null) {
            append(child.getElement());
        }
    }

    @Override
    public ElementBuilder<String> createNode(String tagName) {
        return new StringElementBuilder(tagName);
    }

    @Override
    public StringElementBuilder createChild(String tagName) {
        return (StringElementBuilder)super.createChild(tagName);
    }

    @Override
    protected String create(CharSequence node) {
        return node == null ? "" : node.toString();
    }

    @Override
    protected NodeBuilder<String> wrapChars(CharSequence body) {
        return new StringElementBuilder(body);
    }

    @Override
    protected BiFunction<String, Boolean, NodeBuilder<String>> getCreator() {
        return StringElementBuilder::new;
    }

    @Override
    protected StyleApplier createStyleApplier() {
        return new StyleApplier() {
            @Override
            protected void removeStyle(String element, String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void setStyle(String element, String key, String value) {
                // nothing we can do to an immutable object!
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected void clearChildren(String element) {
        if (children != null) {
            // tricky tricky...
            int was = 0;
            assert ((was = el.length()) >= children.getElement().length());
            el = el.replace(children.getElement(), "");
            assert ((was != el.length())) : "Tried to remove a child we did not contain";
        }
    }
}
