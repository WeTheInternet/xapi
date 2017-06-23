package xapi.ui.api.component;

import xapi.model.api.Model;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface ModelComponentMixin <El, Mod extends Model> {

    /**
     * DOM nodes can stash their model IDs as string to this attribute
     */
    String MODEL_ATTR_NAME = "data-model-id";
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

}
