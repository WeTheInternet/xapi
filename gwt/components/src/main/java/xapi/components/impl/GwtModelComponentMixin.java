package xapi.components.impl;

import elemental.dom.Element;
import xapi.ui.api.component.ModelComponentMixin;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface GwtModelComponentMixin <El extends Element> extends ModelComponentMixin<El> {

    @Override
    default String getModelId(El element) {
        final Object dataset = element.getDataset().at("model-id");
        if (dataset != null) {
            return String.valueOf(dataset);
        }
        return element.getId();
    }

}
