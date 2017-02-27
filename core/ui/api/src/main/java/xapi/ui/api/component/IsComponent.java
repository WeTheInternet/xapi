package xapi.ui.api.component;

import xapi.fu.MappedIterable;

/**
 * Successor to xapi-components module's IsWebComponent interface,
 * as this type relies on cross-platform {@link xapi.ui.api.UiNode},
 * instead of raw usage of GWT Element class.
 *
 * In almost all cases, your UiNode will extend UiElement,
 * and all the complex generics and wiring will be generated for you.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public interface IsComponent
    <
        RawElement,
        Self extends IsComponent<RawElement, Self>
    >
{

    RawElement getElement();

    Self getUi();

    IsComponent<?, ?> getParentComponent();

    MappedIterable<IsComponent<?, ?>> getChildComponents();

    void setParentComponent(IsComponent<?, ?> parent);

    void addChildComponent(IsComponent<?, ?> child);
}
