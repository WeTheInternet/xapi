package xapi.ui.api.component;

import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.ui.api.ElementBuilder;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface ModelComponentMixin <El, Mod extends Model> {

    /**
     * DOM nodes can stash their model IDs as string to this attribute
     */
    String MODEL_ATTR_NAME = "data-model-id";
    String FIELD_MODEL_ATTR_NAME = "MODEL_ATTR_NAME"; // for code generators to reference field above
    /**
     * This is the property name to use when extracting from:
     * var ident = element.data.modelId;
     */
    String MODEL_DATA_NAME = "modelId";

    String getModelId(El el);

    String getModelType();

    Mod createModel();

    default String getModelPropertyName() {
        return X_String.firstCharToLowercase(getModelType());
    }

    default void applyAttribute(ElementBuilder<?> into, Mod mod) {
        ModelKey key = mod.getKey();
        if (key == null) {
            key = X_Model.newKey(mod.getType());
            mod.setKey(key);
        }
        if (!key.isComplete()) {
            String id = into.getId(true);
            key.setId(id);
        }
        X_Model.cache().cacheModel(mod, ignored->{});
        into.setAttribute(ModelComponentMixin.MODEL_ATTR_NAME, shortenKey(key));
    }

    String METHOD_SHORTEN = "shortenKey"; // this is here so if you rename the method below, you'll notice this in code review!
    static String shortenKey(ModelKey key) {
        if (key == null) {
            // ModelKey treats both "" and null as empty, so this should suffice...
            return "x-";
        }
        if (key.getParent() == null) {
            // allow shorthand for simple keys; x-$id
            return "x-" + X_String.notNull(key.getId());
        }
        // if there's any kind of a parent in the key, we'll require the whole thing
        return X_Model.keyToString(key);
    }

    static String rehydrate(String type, String key) {
        if (X_String.isEmptyTrimmed(key) || "null".equals(key) || "undefined".equals(key)) {
            key = "x-";
        }
        if (key.startsWith("x-")) {
            final ModelKey k = X_Model.newKey(type).setId(key.substring(2));
            return X_Model.keyToString(k);
        }
        return key;
    }
}
