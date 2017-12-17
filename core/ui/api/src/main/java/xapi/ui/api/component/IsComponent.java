package xapi.ui.api.component;

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
        /**
         * Specific native node type.
         */
        El
    >
{

    El getElement();

    String getRefName();

}
