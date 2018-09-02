package xapi.components.impl;

import elemental2.dom.HTMLElement;
import xapi.model.api.Model;
import xapi.ui.api.component.IsModelComponent;
import xapi.ui.api.component.ModelComponentMixin;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface GwtModelComponentMixin<El extends HTMLElement, Mod extends Model>
    extends ModelComponentMixin<El, Mod>, IsModelComponent<El, Mod> {

    @Override
    default String getModelId(El element) {
        final Object dataset = element.dataset.get(MODEL_DATA_NAME);
        if (dataset != null) {
            return ModelComponentMixin.rehydrate(getModelType(), String.valueOf(dataset));
        }
        return element.id;
    }

}
