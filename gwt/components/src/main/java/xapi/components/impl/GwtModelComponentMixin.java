package xapi.components.impl;

import elemental.dom.Element;
import xapi.model.api.Model;
import xapi.ui.api.component.ModelComponentMixin;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface GwtModelComponentMixin <El extends Element, Mod extends Model> extends ModelComponentMixin<El, Mod> {

    @Override
    default String getModelId(El element) {
        final Object dataset = element.getDataset().at("modelId");
        if (dataset != null) {
            return String.valueOf(dataset);
        }
        return element.getId();
    }

}
