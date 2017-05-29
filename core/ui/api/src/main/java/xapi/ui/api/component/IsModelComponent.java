package xapi.ui.api.component;

import xapi.fu.iterate.SizedIterable;
import xapi.model.api.Model;

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
public interface IsModelComponent
    <
        /**
         * Raw node; the lowest common denominator of generated api.
         * Your parent and child nodes must be derived from this type
         */
        Node,
        /**
         * Specific node; the final incarnation of generated api
         */
        El extends Node,
        M extends Model
    >
    extends IsComponent<Node, El>, ModelComponentMixin<El>
{

    M getModel();
}
