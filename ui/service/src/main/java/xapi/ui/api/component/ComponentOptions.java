package xapi.ui.api.component;

import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.ui.api.ElementBuilder;

/**
 * Used as the core of a builder-like object, used to pass
 * parameters into a component; in particular, web components
 * have a race condition around creating the raw element
 * and creating the component object wrapping that element.
 *
 * When the element is created via raw HTML, it will need
 * to use sensible defaults and/or mutable properties,
 * and will have to create a default component class.
 *
 * When instantiating a component class (custom or standard),
 * we need a way to tell the element we create about the existing object.
 * We do this by passing an instance of {@link ComponentOptions} into
 * the constructor of said element.
 *
 * Using this abstraction, both the component and element can
 * have immutable references to each other.
 *
 * Any other immutable properties will need to be passed in via ComponentOptions,
 * and subclasses, particularly generated ones, will serve as your
 * java-friendly means to construct custom elements
 * (plus, the use of this abstraction can allow reuse across platforms,
 * as this type has no direct dependencies on raw html types).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public class ComponentOptions <E, C extends IsComponent<E>> {

    protected In1Out1<E, C> component;
    protected C existing;
    private ElementBuilder<E> builder;

    public ComponentOptions<E, C> withComponent(C component) {
        existing = component;
        this.component = Immutable.immutable1(component).ignoreIn1();
        return this;
    }

    public C newComponent(E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        return component.io(element);
    }

    public ComponentOptions<E, C> withComponent(In1Out1<E, C> componentFactory) {
        this.component = componentFactory;
        return this;
    }

    public boolean hasExisting() {
        return existing != null;
    }

    public C getExisting() {
        return existing;
    }

    public boolean needsComponent() {
        return component == null;
    }

    public ComponentOptions<E, C> withBuilder(ElementBuilder<E> b) {
        this.builder = b;
        return this;
    }

    public boolean hasBuilder() {
        return builder != null;
    }

    public ElementBuilder<E> getBuilder() {
        return builder;
    }

    public E build() {
        if (builder == null) {
            return null;
        }
        if (existing != null) {
            if (existing.isResolving(builder)) {
                // the builder is already resolving; we'll return null here,
                // to let the calling code pass through to native element constructor.
                return null;
            }
        }
        return builder.getElement();
    }

    public E create(ComponentConstructor<E, C> i) {
        return i.constructElement(this);
    }
}
