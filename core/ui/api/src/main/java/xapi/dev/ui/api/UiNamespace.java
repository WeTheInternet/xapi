package xapi.dev.ui.api;

import xapi.dev.source.CanAddImports;
import xapi.fu.Maybe;
import xapi.ui.api.StyleCacheService;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiElementBuilder;
import xapi.ui.api.component.IsModelComponent;
import xapi.ui.api.style.HasStyleResources;
import xapi.ui.impl.StubUiElementInjector;
import xapi.ui.impl.UiServiceImpl;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/10/17.
 */
public interface UiNamespace {

    String  TYPE_SELF = "Self",
            TYPE_API = "Api",
            TYPE_BASE = "Base",
            TYPE_IMPL = "Impl",
            TYPE_NODE = "Node",
            TYPE_ELEMENT = "El",
            TYPE_MODEL = "Mod",
            TYPE_ELEMENT_BUILDER = "ElBuilder"
    ;
    String VAR_ELEMENT = "el",
           VAR_BUILDER = "b",
           VAR_TAG_NAME = "TAG_NAME"
    ;

    String  ATTR_ID = "id",
            ATTR_REF = "ref",
            ATTR_MODEL = "model",
            ATTR_DATA = "data",
            ATTR_STYLE = "style",
            ATTR_HREF = "href"
    ;

    String  METHOD_BUILDER = "builder",
            METHOD_NEW_BUILDER = "newBuilder",
            METHOD_AS_BUILDER = "asBuilder",
            METHOD_NEW_MODEL = "newModel",
            METHOD_NEW_MODEL_BUILDER = "modelBuilder",
            METHOD_NEW_MODEL_KEY = "newKey"
    ;

    default Maybe<String> loadFromNamespace(String name, CanAddImports imports) {
        switch (name) {
            case TYPE_SELF:
            case TYPE_API:
            case TYPE_BASE:
            case TYPE_IMPL:
                return Maybe.immutable(name);
            case TYPE_NODE:
                return Maybe.immutable(
                    getNodeType(imports)
                );
            case TYPE_ELEMENT:
                return Maybe.immutable(
                    getElementType(imports)
                );
            case TYPE_ELEMENT_BUILDER:
                return Maybe.immutable(
                    getElementBuilderType(imports)
                );
        }
        return Maybe.not();
    }

    class DefaultUiNamespace implements UiNamespace {
        public static final DefaultUiNamespace DEFAULT = new DefaultUiNamespace();
        private DefaultUiNamespace() {}
    }

    default String getElementType(CanAddImports importer) {
        return importer.addImport(UiElement.class);
    }

    default String getElementBuilderType(CanAddImports importer) {
        // dirty hack... the generic UiElementBuilder is non-functional anyway (we rely on implementation specific generators
        // to specify usable element builder types).
        return importer.addImport(UiElementBuilder.class) + "<" + getElementType(importer) + ">";
    }

    default String getElementInjectorType(CanAddImports importer) {
        return importer.addImport(StubUiElementInjector.class);
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

    // TODO: add public names for the following methods,
    // as well as entries in {@link UiNamespace#loadFromNamespace}

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

    default String getModelComponentMixin(CanAddImports importer) {
        return importer.addImport(IsModelComponent.class);
    }
}
