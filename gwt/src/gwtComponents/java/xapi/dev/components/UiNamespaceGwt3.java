package xapi.dev.components;

import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLStyleElement;
import elemental2.dom.Node;
import xapi.components.impl.GwtModelComponentMixin;
import xapi.dev.source.CanAddImports;
import xapi.elemental.api.ElementalBuilder;
import xapi.elemental.api.ElementalInjector;
import xapi.elemental.impl.Gwt3ServiceDefault;
import xapi.ui.api.ElementInjector;
import xapi.ui.html.api.GwtStyles;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/12/17.
 */
public class UiNamespaceGwt3 extends UiNamespaceGwt {

    public UiNamespaceGwt3() {
    }

    @Override
    public String getElementBuilderType(CanAddImports importer) {
        return importer.addImport(ElementalBuilder.class)
             + "<" + getElementType(importer) + ">";
    }

    @Override
    public String getElementInjectorType(CanAddImports importer) {
        return importer.addImport(ElementalInjector.class);
    }

    @Override
    public String getNodeType(CanAddImports importer) {
        return importer.addImport(Node.class);
    }

    @Override
    public String getElementType(CanAddImports importer) {
        return importer.addImport(HTMLElement.class);
    }

    @Override
    public String getStyleElementType(CanAddImports importer) {
        return importer.addImport(HTMLStyleElement.class);
    }

    @Override
    public String getStyleCacheType(CanAddImports importer) {
        return importer.addImport(Gwt3ServiceDefault.class);
    }

    @Override
    public String getServiceType(CanAddImports importer) {
        return importer.addImport(Gwt3ServiceDefault.class);
    }

    @Override
    public String getStyleResourceType(CanAddImports importer) {
        return importer.addImport(GwtStyles.class);
    }

    @Override
    public String getModelComponentMixin(CanAddImports importer) {
        return importer.addImport(GwtModelComponentMixin.class);
    }
}
