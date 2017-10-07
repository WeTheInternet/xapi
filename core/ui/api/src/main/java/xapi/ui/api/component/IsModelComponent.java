package xapi.ui.api.component;

import xapi.fu.iterate.SizedIterable;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.service.ModelCache;

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
    extends IsComponent<Node, El>, ModelComponentMixin<El, M>
{

    M getModel();

    /**
     * @return X_Model.cache(), unless you override with a local cache
     * (for example, if you want to perform server side rendering,
     * and wish to supply a cache that you can serialize along with any DOM).
     */
    default ModelCache cache() {
        return X_Model.cache();
    }
}
