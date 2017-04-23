package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeArguments;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.ui.api.NodeBuilder;

import java.util.Collections;

import static xapi.fu.Lazy.deferAll;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public abstract class GeneratedUiLayer extends GeneratedJavaFile {

    public enum ImplLayer {
        Api, Super, Base, Impl, Mixin
    }

    private final ImplLayer layer;

    protected Lazy<GeneratedUiModel> model = deferAll(
        GeneratedUiModel::new,
        this::getOwner,
        this::getPackageName,
        this::getNameForModel
    );

    public Maybe<GeneratedUiLayer> getSuperType() {
        return Maybe.nullable((GeneratedUiLayer) superType);
    }

    protected String getNameForModel() {
        return getTypeName(); // you may want to suffix your models.
    }

    private String nameNode, nameElement, nameElementBuilder, nameStyleService, nameStyleElement, nameBase, nameApi;
    private final StringTo<In1<GeneratedUiImplementation>> abstractMethods;
    private final StringTo<ReferenceType> localDefinitions;

    public GeneratedUiLayer(String pkg, String cls, ImplLayer layer, GeneratedUiComponent owner) {
        this(null, pkg, cls, layer, owner);
    }

    @SuppressWarnings("unchecked")
    public GeneratedUiLayer(
        GeneratedUiLayer superType,
        String pkg,
        String cls,
        ImplLayer layer,
        GeneratedUiComponent owner
    ) {
        super(owner, superType, pkg, cls);
        this.layer = layer;
        abstractMethods = X_Collect.newStringMap(In1.class);
        localDefinitions = X_Collect.newStringMap(ReferenceType.class);
    }

    public String getModelName() {
        return model.out1().getWrappedName();
    }

    public GeneratedUiModel getModel() {
        return model.out1();
    }

    protected abstract IsTypeDefinition definition();

    public void addLocalDefinition(String sysName, ReferenceType param) {
        localDefinitions.put(sysName, param);
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> source = super.createSource();
        final IsTypeDefinition definition = definition();
        if (definition != null) {
            source.setClassDefinition(definition.toDefinition(), false);
            source.setPackage(definition.getPackage());
        }
        return source;
    }

    public boolean hasModel() {
        return model.isResolved();
    }

    public String getNodeType(UiNamespace namespace) {
        if (nameNode == null) {
            nameNode = reserveName(UiNamespace.TYPE_NODE,
                "N", "_Node", "_N"
            );
        }
        return nameNode;
    }

    public String getElementType(UiNamespace namespace) {
        if (nameElement == null) {
            nameElement = reserveName(UiNamespace.TYPE_ELEMENT,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "E", "Ele", "Element"
            );
        }
        return nameElement;
    }

    public String getBaseType(UiNamespace namespace) {
        if (nameBase == null) {
            nameBase = reserveName(UiNamespace.TYPE_BASE,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "B", "BaseType"
            );
        }
        return nameBase;
    }

    public String getApiType(UiNamespace namespace) {
        if (nameApi == null) {
            nameApi = reserveName(UiNamespace.TYPE_API,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "A", "ApiType"
            );
        }
        return nameApi;
    }

    protected String reserveName(String sysName, String ... backups) {
        final String name = getSource().getImports().reserveSimpleName(
            X_Fu.concat(new String[]{sysName}, backups)
        );
        final GeneratedTypeParameter generic = getGenericInfo().getOrCreateGeneric(sysName);
        if (this.layer == ImplLayer.Api) {
            generic.setExposed(true);
        }
        generic.setLayerName(this.layer, name);
        return name;
    }

    public String getElementBuilderType(UiNamespace namespace) {
        if (nameElementBuilder == null) {
            if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                // When in api/base layer, we need to use generics instead of concrete types.
                nameElementBuilder = getSource().getImports().reserveSimpleName(
                    UiNamespace.TYPE_ELEMENT_BUILDER,
                    "ElementBuilder",
                    "Builder",
                    "EB"
                );
                String nodeBuilder = getSource().addImport(NodeBuilder.class);
                final String elementType = getElementType(namespace);
                final GeneratedUiGenericInfo generics = getOwner().getGenericInfo();
                final GeneratedTypeParameter generic = generics
                    .setLayerName(UiNamespace.TYPE_ELEMENT_BUILDER, layer, nameElementBuilder);

                final ClassOrInterfaceType nodeType = new ClassOrInterfaceType(nodeBuilder);
                nodeType.setTypeArguments(TypeArguments.withArguments(
                    Collections.singletonList(new ClassOrInterfaceType(elementType))
                ));
                generic.absorb(new TypeParameter(nameElementBuilder, nodeType));
            } else {
                nameElementBuilder = namespace.getElementBuilderType(getSource());
            }
        }
        // Use the concrete type
        return nameElementBuilder;
    }

    public boolean hasGenerics() {
        return getOwner().getGenericInfo().hasGenerics(layer);
    }

    public MappedIterable<String> getGenericNames() {
        return getGenericInfo().getTypeParameterNames(layer);
    }

    public MappedIterable<GeneratedTypeParameter> getTypeParameters() {
        return getGenericInfo().getTypeParameters(layer);
    }

    public boolean hasGeneric(String self) {
        return getGenericInfo().hasTypeParameter(layer, self);
    }

    protected GeneratedUiGenericInfo getGenericInfo() {
        return getOwner().getGenericInfo();
    }

    public String getTypeWithGenerics(ImplLayer forLayer, UiGeneratorService generator, UiNamespace namespace, CanAddImports out) {
        final MappedIterable<GeneratedTypeParameter> myParams = getGenericInfo().getTypeParameters(layer);
        return getWrappedName() + (
            myParams.isEmpty() ? "" :
                    myParams
                        .map(s->s.computeDeclaration(this, forLayer, generator, namespace, out))
                        .join("<", ", ", ">")
        );
    }

    public ImplLayer getLayer() {
        return layer;
    }

    public String getGenericValue(GeneratedTypeParameter param) {
        // Check if this layer has a type provider for the requested type param.

        return getOwner().addGeneric(param.getSystemName(), param.getType())
            .setExposed(param.isExposed())
            .absorb(param.getType());
    }
}
