package xapi.dev.components;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.StyleElement;
import xapi.dev.source.CanAddImports;
import xapi.dev.ui.UiNamespace;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.ui.html.api.GwtStyles;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/12/17.
 */
public class UiNamespaceGwt implements UiNamespace {

    public UiNamespaceGwt() {
        System.out.println();
    }

    @Override
    public String getElementBuilderType(CanAddImports importer) {
        return importer.addImport(PotentialNode.class)
             + "<" + getElementType(importer) + ">";
    }

    @Override
    public String getNodeType(CanAddImports importer) {
        return importer.addImport(Node.class);
    }

    @Override
    public String getElementType(CanAddImports importer) {
        return importer.addImport(Element.class);
    }

    @Override
    public String getStyleElementType(CanAddImports importer) {
        return importer.addImport(StyleElement.class);
    }

    @Override
    public String getStyleCacheType(CanAddImports importer) {
        return importer.addImport(ElementalService.class);
    }

    @Override
    public String getServiceType(CanAddImports importer) {
        return importer.addImport(ElementalService.class);
    }

    @Override
    public String getStyleResourceType(CanAddImports importer) {
        return importer.addImport(GwtStyles.class);
    }
}
