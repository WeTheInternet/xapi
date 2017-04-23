package xapi.ui.api.component;

import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.util.X_Util;

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

    default <E, C extends IsComponent<E, C>> C getRefOrNull(String refName){
        final Maybe is = getRef(IsComponent.class, refName);
        return ((Maybe<C>)is).ifAbsentReturn(null);
    }

    default <E, C extends IsComponent<E, C>> Maybe<C> getRef(
        Class<C> clazz, String refName) {

        return getChildComponents()
            .map1(X_Fu::cast, clazz)
            .firstMatch(c -> X_Util.equal(
            c.getRefName(),
            refName
        ));
    }

    String getRefName();

}
