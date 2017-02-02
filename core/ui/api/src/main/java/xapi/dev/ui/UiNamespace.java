package xapi.dev.ui;

import xapi.dev.source.CanAddImports;
import xapi.ui.api.StyleCacheService;
import xapi.ui.api.UiElement;
import xapi.ui.api.style.HasStyleResources;
import xapi.ui.impl.StringElementBuilder;
import xapi.ui.impl.UiServiceImpl;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/10/17.
 */
public interface UiNamespace {

    String ATTR_ID = "id";
    String ATTR_REF = "ref";
    String ATTR_MODEL = "model";
    String ATTR_DATA = "data";
    String ATTR_STYLE = "style";
    String ATTR_HREF = "href";
    String EL_BOX = "box";
    String EL_BUTTON = "button";
    String EL_INPUT = "input";

    class DefaultUiNamespace implements UiNamespace {
        protected DefaultUiNamespace() {}
    }

    DefaultUiNamespace DEFAULT_UI_NAMESPACE = new DefaultUiNamespace();

    default String getElementType(CanAddImports importer) {
        return importer.addImport(UiElement.class);
    }

    default String getElementBuilderType(CanAddImports importer) {
        return importer.addImport(StringElementBuilder.class);
    }

    default String getElementBuilderConstructor(CanAddImports importer) {
        return "new " + getElementBuilderType(importer) + "()";
    }

    /**
     * @return The root abstract type of a raw,
     * platform-specific ui element node.
     *
     * Web will use Node, not Element,
     * as we want to understand all dom node types.
     */
    default String getNodeType(CanAddImports importer) {
        return importer.addImport(UiElement.class);
    }

    /**
     * @return
     */
    default String getStyleElementType(CanAddImports importer) {
        return importer.addImport(UiElement.class);
    }

    default String getServiceType(CanAddImports importer) {
        return importer.addImport(UiServiceImpl.class);
    }

    default String getStyleResourceType(CanAddImports importer) {
        return importer.addImport(HasStyleResources.class);
    }

    default String getBaseStyleResourceType(CanAddImports importer) {
        return getStyleResourceType(importer);
    }

    default String getStyleCacheType(CanAddImports importer) {
        return importer.addImport(StyleCacheService.class);
    }

}
